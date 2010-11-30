/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainImpl;
import org.jboss.as.deployment.chain.DeploymentChainService;
import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderService;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.server.mgmt.ServerConfigurationPersister;
import org.jboss.as.server.mgmt.ServerConfigurationPersisterImpl;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentManagerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentRepositoryImpl;
import org.jboss.as.server.standalone.deployment.DeploymentScannerFactoryService;
import org.jboss.as.server.standalone.management.StandaloneServerManagementServices;
import org.jboss.as.services.net.SocketBindingManager;
import org.jboss.as.services.net.SocketBindingManagerService;
import org.jboss.as.version.Version;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerStartTask implements ServerTask, Serializable, ObjectInputValidation {

    public static final ServiceName AS_SERVER_SERVICE_NAME = ServiceName.JBOSS.append("as", "server");

    private static final long serialVersionUID = -8505496119636153918L;

    private final String serverName;
    private final int portOffset;
    private final List<ServiceActivator> startServices;
    private final List<AbstractServerModelUpdate<?>> updates;
    private final ServerEnvironment providedEnvironment;

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    /** Constructor variant for use by the HostController */
    public ServerStartTask(final String serverName, final int portOffset, final List<ServiceActivator> startServices, final List<AbstractServerModelUpdate<?>> updates) {
        this(serverName, portOffset, startServices, updates, null);
        if (serverName == null || serverName.length() == 0) {
            throw new IllegalArgumentException("Server name " + serverName + " is invalid; cannot be null or blank");
        }
    }

    /** Constructor variant for use by StandaloneServer */
    public ServerStartTask(final int portOffset, final List<ServiceActivator> startServices, final List<AbstractServerModelUpdate<?>> updates, final ServerEnvironment environment) {
        this(null, portOffset, startServices, updates, environment);
    }

    private ServerStartTask(final String serverName, final int portOffset, final List<ServiceActivator> startServices, final List<AbstractServerModelUpdate<?>> updates, final ServerEnvironment environment) {
        this.serverName = serverName;
        this.portOffset = portOffset;
        this.startServices = startServices;
        this.updates = updates;
        this.providedEnvironment = environment;
    }

    public void run(final List<ServiceActivator> runServices) {
        if (serverName != null) {
            MDC.put("process", "server-" + serverName);

            log.infof("Starting server \"%s\"", serverName);
        }
        else {
            MDC.put("process", "standalone-server");

            log.infof("Starting standalone server");
        }
        final ServiceContainer container = ServiceContainer.Factory.create();
        final int threads = Runtime.getRuntime().availableProcessors();
        container.setExecutor(new ThreadPoolExecutor(threads, threads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>()));

        final ServerStartupListener serverStartupListener = new ServerStartupListener(createListenerCallback());
        final BatchBuilder batchBuilder = container.batchBuilder();
        batchBuilder.addListener(serverStartupListener);

        // First-stage (boot) services

        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext() {
            public ServiceTarget getServiceTarget() {
                return container;
            }

            public ServiceRegistry getServiceRegistry() {
                return container;
            }
        };

        // Root service
        batchBuilder.addService(AS_SERVER_SERVICE_NAME, Service.NULL)
        .setInitialMode(ServiceController.Mode.ACTIVE)
        .install();

        // Services specified by the creator of this object
        for (ServiceActivator service : startServices) {
            service.activate(serviceActivatorContext);
        }

        // Next-stage services

        // Initial model
        final ServerModel serverModel = new ServerModel(serverName, portOffset);

        final Properties systemProperties = System.getProperties();
        final ServerEnvironment environment = providedEnvironment != null
                        ? providedEnvironment
                        : new ServerEnvironment(systemProperties, serverName, false);

        log.info("Activating core services");

        // The server controller
        // TODO make ServerConfigurationPersister internal
        // TODO share thread pool
        ServerControllerImpl serverController = new ServerControllerImpl(serverModel, container, environment.isStandalone());
        batchBuilder.addService(ServerController.SERVICE_NAME, serverController)
            .addDependency(ServerConfigurationPersister.SERVICE_NAME, ServerConfigurationPersister.class, serverController.getConfigurationPersisterValue())
            .addInjection(serverController.getExecutorValue(), Executors.newCachedThreadPool())
            .install();

        // Server environment services
        ServerEnvironmentServices.addServices(environment, batchBuilder);

        // Deployment repository
        ServerDeploymentRepositoryImpl.addService(batchBuilder);

        // Graceful shutdown
        ShutdownHandlerImpl.addService(batchBuilder);

        // Server model service - TODO: replace with ServerController
        ServerModelService.addService(serverModel, batchBuilder);

        // Server deployment manager - TODO: move into startServices, only start in standalone mode
        ServerDeploymentManagerImpl.addService(serverModel, container, batchBuilder);

        // Server configuration persister - TODO: move into startServices, only start in standalone mode
        ServerConfigurationPersisterImpl.addService(serverModel, batchBuilder);

        // Server deployment scanner factory
        DeploymentScannerFactoryService.addService(batchBuilder);

        batchBuilder.addService(SocketBindingManager.SOCKET_BINDING_MANAGER,
                new SocketBindingManagerService(portOffset)).setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        // Activate deployment module loader
        batchBuilder.addService(ClassifyingModuleLoaderService.SERVICE_NAME, new ClassifyingModuleLoaderService())
            .install();

        // todo move elsewhere...
        final DeploymentChain deploymentChain = new DeploymentChainImpl();
        deploymentChain.addProcessor(new DeploymentModuleLoaderProcessor(new DeploymentModuleLoaderImpl()), Phase.DEPLOYMENT_MODULE_LOADER_PROCESSOR);
        batchBuilder.addService(DeploymentChain.SERVICE_NAME, new DeploymentChainService(deploymentChain))
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();

        new JarDeploymentActivator().activate(deploymentChain);

        for (AbstractServerModelUpdate<?> update : updates) {
            try {
                serverModel.update(update);
            } catch (UpdateFailedException e) {
                throw new IllegalStateException("Failed to start server", e);
            }
        }

        final BootUpdateContext context = new BootUpdateContext() {
            public BatchBuilder getBatchBuilder() {
                return batchBuilder;
            }

            public ServiceContainer getServiceContainer() {
                return container;
            }

            public void addDeploymentProcessor(DeploymentUnitProcessor processor, long priority) {
                deploymentChain.addProcessor(processor, priority);
            }
        };

        for (AbstractServerModelUpdate<?> update : updates) {
            if(!update.isDeploymentUpdate()) {
                update.applyUpdateBootAction(context);
            }
        }

        DeploymentUpdateService.addService(batchBuilder, updates, serverStartupListener);

        StandaloneServerManagementServices.addServices(serverModel, container, batchBuilder);

        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new IllegalStateException("Failed to install boot services", e);
        }
    }

    ServerStartupListener.Callback createListenerCallback() {
        return new ServerStartupListener.Callback() {
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices) {
                if(serviceFailures.isEmpty()) {
                    log.infof("JBoss AS %s \"%s\" started in %dms. - Services [Total: %d, On-demand: %d. Started: %d]", Version.AS_VERSION, Version.AS_RELEASE_CODENAME, Long.valueOf(elapsedTime), Integer.valueOf(totalServices), Integer.valueOf(onDemandServices), Integer.valueOf(startedServices));
                } else {
                    final StringBuilder buff = new StringBuilder(String.format("JBoss AS server start failed. Attempted to start %d services in %dms", Integer.valueOf(totalServices), Long.valueOf(elapsedTime)));
                    buff.append("\nThe following services failed to start:\n");
                    for(Map.Entry<ServiceName, StartException> entry : serviceFailures.entrySet()) {
                        buff.append(String.format("\t%s => %s\n", entry.getKey(), entry.getValue().getMessage()));
                    }
                    log.error(buff.toString());
                }
            }
        };
    }

    public void validateObject() throws InvalidObjectException {
        if (serverName == null) {
            throw new InvalidObjectException("serverName is null");
        }
        if (portOffset < 0) {
            throw new InvalidObjectException("portOffset is out of range");
        }
        if (updates == null) {
            throw new InvalidObjectException("updates is null");
        }
        if (startServices == null) {
            throw new InvalidObjectException("startServices is null");
        }
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        ois.registerValidation(this, 100);
    }
}
