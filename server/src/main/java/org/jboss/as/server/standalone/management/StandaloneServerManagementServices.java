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

import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.ServerDeploymentRepository;
import org.jboss.as.model.ManagementElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.mgmt.ServerConfigurationPersister;
import org.jboss.as.server.mgmt.ShutdownHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentManager;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneServerManagementServices {

    public static void addServices(ServerModel serverModel, ServiceContainer container, BatchBuilder batchBuilder) {
        ManagementElement managementElement = serverModel.getManagementElement();
        if (managementElement != null) {

            final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
            batchBuilder.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
                    .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()), NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
                    .addInjection(managementCommunicationService.getPortInjector(), managementElement.getPort())
                    .addInjection(managementCommunicationService.getExecutorServiceInjector(), Executors.newCachedThreadPool())
                    .addInjection(managementCommunicationService.getThreadFactoryInjector(), Executors.defaultThreadFactory())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            // Handlers
            final ServerControllerOperationHandler clientOperationHandler = new ServerControllerOperationHandler(container);
            batchBuilder.addService(ServerControllerOperationHandler.SERVICE_NAME, clientOperationHandler)
                    .addDependency(Services.JBOSS_SERVER_CONTROLLER, ServerController.class, clientOperationHandler.getServerControllerInjector())
                    .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(clientOperationHandler))
                    .addDependency(ServerConfigurationPersister.SERVICE_NAME, ServerConfigurationPersister.class, clientOperationHandler.getConfigurationPersisterValue())
                    .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, clientOperationHandler.getDeploymentRepositoryInjector())
                    .addDependency(ServerDeploymentManager.SERVICE_NAME_LOCAL, ServerDeploymentManager.class, clientOperationHandler.getDeploymentManagerInjector())
                    .addDependency(ShutdownHandler.SERVICE_NAME, ShutdownHandler.class, clientOperationHandler.getShutdownHandlerValue())
                    // FIXME inject executor
                    .addInjection(clientOperationHandler.getExecutorValue(), Executors.newCachedThreadPool())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
        }
    }

}
