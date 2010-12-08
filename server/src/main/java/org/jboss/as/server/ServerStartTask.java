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
import java.util.Properties;

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
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.server.mgmt.StandaloneServerConfigurationPersister;
import org.jboss.as.server.mgmt.ShutdownHandlerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentManagerImpl;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentRepositoryImpl;
import org.jboss.as.server.standalone.deployment.DeploymentScannerFactoryService;
import org.jboss.as.server.standalone.management.StandaloneServerManagementServices;
import org.jboss.as.services.net.SocketBindingManager;
import org.jboss.as.services.net.SocketBindingManagerService;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

/**
 * This is the task used by the Host Controller and passed to a Server instance
 * in order to bootstrap it from a remote source process.
 *
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

    public ServerStartTask(final String serverName, final int portOffset, final List<ServiceActivator> startServices, final List<AbstractServerModelUpdate<?>> updates) {
        if (serverName == null || serverName.length() == 0) {
            throw new IllegalArgumentException("Server name " + serverName + " is invalid; cannot be null or blank");
        }
        this.serverName = serverName;
        this.portOffset = portOffset;
        this.startServices = startServices;
        this.updates = updates;
        this.providedEnvironment = new ServerEnvironment(System.getProperties(), false);
    }

    public void run(final List<ServiceActivator> runServices) {
        final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
        bootstrap.start(new Bootstrap.Configuration(), startServices);

        // The server controller

        // Deployment repository
        ServerDeploymentRepositoryImpl.addService(batchBuilder);

        // Server deployment manager - TODO: move into startServices, only start in standalone mode
        ServerDeploymentManagerImpl.addService(serverModel, container, batchBuilder);

        // Server deployment scanner factory
        DeploymentScannerFactoryService.addService(batchBuilder);

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
            public ServiceTarget getServiceTarget() {
                return batchBuilder;
            }

            public ServiceContainer getServiceContainer() {
                throw new UnsupportedOperationException();
            }

            public void addDeploymentProcessor(final Phase phase, DeploymentUnitProcessor processor, int priority) {
                deploymentChain.addProcessor(processor, priority);
            }

            public ServiceRegistry getServiceRegistry() {
                return getServiceContainer();
            }
        };

        DeploymentUpdateService.addService(batchBuilder, updates, serverStartupListener);

        StandaloneServerManagementServices.addServices(serverModel, container, batchBuilder);

        try {
            batchBuilder.install();
        } catch (ServiceRegistryException e) {
            throw new IllegalStateException("Failed to install boot services", e);
        }
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
