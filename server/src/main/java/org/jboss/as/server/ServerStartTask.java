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
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.server.mgmt.ServerConfigurationPersisterImpl;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentManagerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentRepositoryImpl;
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

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    public ServerStartTask(final String serverName, final int portOffset, final Runnable logConfigurator, final List<ServiceActivator> startServices, final List<AbstractServerModelUpdate<?>> updates) {
        this.serverName = serverName;
        this.portOffset = portOffset;
        this.logConfigurator = logConfigurator;
        this.startServices = startServices;
        this.updates = updates;
    }

    public void run(final List<ServiceActivator> startServices) {
        if (logConfigurator != null) logConfigurator.run();
        log.infof("Starting server \"%s\"", serverName);
        final ServiceContainer container = ServiceContainer.Factory.create();

        final ServerStartupListener serverStartupListener = new ServerStartupListener(createListenerCallback());

        // First-stage (boot) services
        final BatchBuilder bootBatchBuilder = container.batchBuilder();
        bootBatchBuilder.addListener(serverStartupListener);
        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext() {
            public BatchBuilder getBatchBuilder() {
                return bootBatchBuilder;
            }
        };

        // Root service
        final BatchServiceBuilder<Void> builder = bootBatchBuilder.addService(ServiceName.JBOSS.append("as", "server"), Service.NULL);
        builder.setInitialMode(ServiceController.Mode.IMMEDIATE);

        // Services specified by the creator of this object
        for (ServiceActivator service : this.startServices) {
            service.activate(serviceActivatorContext);
        }

        // Services specified to this method
        for (ServiceActivator service : startServices) {
            service.activate(serviceActivatorContext);
        }

        // Install the boot services
        try {
            bootBatchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new IllegalStateException("Failed to install boot services", e);
        }

        // Next-stage services
        final BatchBuilder batchBuilder = container.batchBuilder();
        batchBuilder.addListener(serverStartupListener);

        // Initial model
        final ServerModel serverModel = new ServerModel(serverName, portOffset);

        final Properties systemProperties = System.getProperties();
        final ServerEnvironment environment = new ServerEnvironment(systemProperties, serverName, false);

        log.info("Activating core services");

        // Server env
        ServerEnvironmentService.addService(environment, batchBuilder);

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

        for (AbstractServerModelUpdate<?> update : updates) {
            try {
                serverModel.update(update);
            } catch (UpdateFailedException e) {
                throw new IllegalStateException("Failed to start server", e);
            }
        }

        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new IllegalStateException("Failed to start server", e);
        }

        final BatchBuilder updatesBatchBuilder = container.batchBuilder();

        final UpdateContext context = new UpdateContext() {
            public BatchBuilder getBatchBuilder() {
                return updatesBatchBuilder;
            }

            public ServiceContainer getServiceContainer() {
                return container;
            }
        };

        for (AbstractServerModelUpdate<?> update : updates) {
            update.applyUpdateBootAction(context);
        }

        try {
            updatesBatchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new IllegalStateException("Failed to install boot services", e);
        }
        serverStartupListener.finish();
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
