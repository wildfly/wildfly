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

package org.jboss.as.server.standalone.management;

import java.util.concurrent.Executors;

import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ManagementElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.ServerController;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.as.services.net.NetworkInterfaceService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneServerManagementServices {

    public static void addServices(ServerModel serverModel, ServiceContainer container, BatchBuilder batchBuilder) {
        ManagementElement managementElement = serverModel.getManagementElement();
        if (managementElement != null) {
            final ServerControllerImpl serverController = new ServerControllerImpl(container, serverModel);
            batchBuilder.addService(ServerController.SERVICE_NAME, serverController);

            final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
            batchBuilder.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
                    .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
                    .addInjection(managementCommunicationService.getPortInjector(), managementElement.getPort())
                    .addInjection(managementCommunicationService.getExecutorServiceInjector(), Executors.newCachedThreadPool())
                    .addInjection(managementCommunicationService.getThreadFactoryInjector(), Executors.defaultThreadFactory())
                    .setInitialMode(ServiceController.Mode.ACTIVE);
            // Handlers
            final ServerControllerOperationHandler serverControllerOperationHandler = new ServerControllerOperationHandler();
            batchBuilder.addService(ServerControllerOperationHandler.SERVICE_NAME, serverControllerOperationHandler)
                    .addDependency(ServerController.SERVICE_NAME, ServerController.class, serverControllerOperationHandler.getServerControllerInjector())
                    .addInjection(serverControllerOperationHandler.getExecutorServiceInjector(), Executors.newScheduledThreadPool(1))
                    .addInjection(serverControllerOperationHandler.getThreadFactoryInjector(), Executors.defaultThreadFactory())
                    .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, serverControllerOperationHandler.getDeploymentRepositoryInjector())
                    .addDependency(ServerDeploymentManager.SERVICE_NAME_LOCAL, ServerDeploymentManager.class, serverControllerOperationHandler.getDeploymentManagerInjector())
                    .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(serverControllerOperationHandler))
                    .setInitialMode(ServiceController.Mode.ACTIVE);
        }
    }

    static class ServerControllerImpl implements ServerController, Service<ServerController> {
        private final ServiceContainer container;
        private final ServerModel serverModel;

        ServerControllerImpl(final ServiceContainer container, final ServerModel model) {
            this.container = container;
            this.serverModel = model;
        }

        /** {@inheritDoc} */
        public ServerModel getServerModel() {
            return serverModel;
        }

        public <R, P> void update(final AbstractServerModelUpdate<R> update, final UpdateResultHandler<R, P> resultHandler, final P param) {
            final UpdateContextImpl updateContext = new UpdateContextImpl(container.batchBuilder(), container);
            synchronized (serverModel) {
                try {
                    serverModel.update(update);
                } catch (UpdateFailedException e) {
                    resultHandler.handleFailure(e, param);
                    return;
                }
            }
            update.applyUpdate(updateContext, resultHandler, param);
        }

        /** {@inheritDoc} */
        public void start(StartContext arg0) throws StartException {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        public ServerController getValue() throws IllegalStateException {
            return this;
        }

        /** {@inheritDoc} */
        public void shutdown() {
            // TODO Auto-generated method stub

        }

        /** {@inheritDoc} */
        public void stop(StopContext arg0) {
            // TODO Auto-generated method stub

        }

        final class UpdateContextImpl implements UpdateContext {

            private final BatchBuilder batchBuilder;
            private final ServiceContainer serviceContainer;

            UpdateContextImpl(final BatchBuilder batchBuilder, final ServiceContainer serviceContainer) {
                this.batchBuilder = batchBuilder;
                this.serviceContainer = serviceContainer;
            }

            public BatchBuilder getBatchBuilder() {
                return batchBuilder;
            }

            public ServiceContainer getServiceContainer() {
                return serviceContainer;
            }
        }
    }
}
