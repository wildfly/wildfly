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
import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderInjector;
import org.jboss.as.deployment.module.ClassifyingModuleLoaderService;
import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderService;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.server.mgmt.ServerConfigurationPersisterImpl;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentManagerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentRepositoryImpl;
import org.jboss.as.services.net.SocketBindingManager;
import org.jboss.as.services.net.SocketBindingManagerService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerStartTask implements ServerTask, Serializable, ObjectInputValidation {

    private static final long serialVersionUID = -8505496119636153918L;

    private final String serverName;
    private final int portOffset;
    private final Runnable logConfigurator;
    private final List<ServiceActivator> startServices;
    private final List<AbstractServerModelUpdate<?>> updates;
    private final ServerEnvironment providedEnvironment;

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    /** Constructor variant for use by the ServerManager */
    public ServerStartTask(final String serverName, final int portOffset, final Runnable logConfigurator, final List<ServiceActivator> startServices, final List<AbstractServerModelUpdate<?>> updates) {
        this(serverName, portOffset, logConfigurator, startServices, updates, null);
    }

    /** Constructor variant for use by StandaloneServer */
    public ServerStartTask(final String serverName, final int portOffset, final Runnable logConfigurator, final List<ServiceActivator> startServices, final List<AbstractServerModelUpdate<?>> updates, final ServerEnvironment environment) {
        this.serverName = serverName;
        this.portOffset = portOffset;
        this.logConfigurator = logConfigurator;
        this.startServices = startServices;
        this.updates = updates;
        this.providedEnvironment = environment;
    }

    public void run(final List<ServiceActivator> startServices) {
        if (logConfigurator != null) logConfigurator.run();
        log.infof("Starting server \"%s\"", serverName);
        final ServiceContainer container = ServiceContainer.Factory.create();

        final ServerStartupListener serverStartupListener = new ServerStartupListener(createListenerCallback());
        final ServerStartBatchBuilder batchBuilder = new ServerStartBatchBuilder(container.batchBuilder(), serverStartupListener);
        batchBuilder.addListener(serverStartupListener);

        // First-stage (boot) services

        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext() {
            public BatchBuilder getBatchBuilder() {
                return batchBuilder;
            }
        };

        // Root service
        final BatchServiceBuilder<Void> builder = batchBuilder.addService(ServiceName.JBOSS.append("as", "server"), Service.NULL);
        builder.setInitialMode(ServiceController.Mode.IMMEDIATE);

        // Services specified by the creator of this object
        for (ServiceActivator service : this.startServices) {
            service.activate(serviceActivatorContext);
        }

        // Services specified to this method
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

        batchBuilder.addService(SocketBindingManager.SOCKET_BINDING_MANAGER,
                new SocketBindingManagerService(portOffset)).setInitialMode(ServiceController.Mode.ON_DEMAND);

        // Activate deployment module loader
        batchBuilder.addService(ClassifyingModuleLoaderService.SERVICE_NAME, new ClassifyingModuleLoaderService());

        final DeploymentModuleLoaderService deploymentModuleLoaderService = new DeploymentModuleLoaderService(new DeploymentModuleLoaderImpl());
        batchBuilder.addService(DeploymentModuleLoaderService.SERVICE_NAME, deploymentModuleLoaderService)
            .addDependency(ClassifyingModuleLoaderService.SERVICE_NAME, ClassifyingModuleLoaderService.class, new ClassifyingModuleLoaderInjector("deployment", deploymentModuleLoaderService));

        // todo move elsewhere...
        new JarDeploymentActivator().activate(new ServiceActivatorContext() {
            public BatchBuilder getBatchBuilder() {
                return batchBuilder;
            }
        });

        for (AbstractServerModelUpdate<?> update : updates) {
            try {
                serverModel.update(update);
            } catch (UpdateFailedException e) {
                throw new IllegalStateException("Failed to start server", e);
            }
        }

        final UpdateContext context = new UpdateContext() {
            public BatchBuilder getBatchBuilder() {
                return batchBuilder;
            }

            public ServiceContainer getServiceContainer() {
                return container;
            }
        };

        for (AbstractServerModelUpdate<?> update : updates) {
            if(!update.isDeploymentUpdate()) {
                update.applyUpdateBootAction(context);
            }
        }

        try {
            serverStartupListener.startBatch(createDeploymentTask(container, serverStartupListener));
            batchBuilder.install();
            serverStartupListener.finishBatch();
        } catch (ServiceRegistryException e) {
            throw new IllegalStateException("Failed to install boot services", e);
        }
    }

    private Runnable createDeploymentTask(final ServiceContainer container, final ServerStartupListener serverStartupListener) {
        return new Runnable() {
            public void run() {
                // Activate deployments once the first batch is complete.
                final ServerStartBatchBuilder deploymentBatchBuilder = new ServerStartBatchBuilder(container.batchBuilder(), serverStartupListener);
                deploymentBatchBuilder.addListener(serverStartupListener);
                serverStartupListener.startBatch(null);

                final UpdateContext context = new UpdateContext() {
                    public BatchBuilder getBatchBuilder() {
                        return deploymentBatchBuilder;
                    }

                    public ServiceContainer getServiceContainer() {
                        return container;
                    }
                };

                for (AbstractServerModelUpdate<?> update : updates) {
                    if(update.isDeploymentUpdate()) {
                        update.applyUpdateBootAction(context);
                    }
                }

                serverStartupListener.finish(); // We have finished adding everything for the server start
                try {
                    deploymentBatchBuilder.install();
                    serverStartupListener.finishBatch();
                } catch (ServiceRegistryException e) {
                    throw new RuntimeException(e); // TODO: better exception handling.
                }
            }
        };
    }

    ServerStartupListener.Callback createListenerCallback() {
        return new ServerStartupListener.Callback() {
            public void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices) {
                if(serviceFailures.isEmpty()) {
                    log.infof("JBoss AS started in %dms. - Services [Total: %d, On-demand: %d. Started: %d]", elapsedTime, totalServices, onDemandServices, startedServices);
                } else {
                    final StringBuilder buff = new StringBuilder(String.format("JBoss AS server start failed. Attempted to start %d services in %dms", totalServices, elapsedTime));
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
        if (logConfigurator == null) {
            throw new InvalidObjectException("logConfigurator is null");
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
