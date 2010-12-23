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

import java.util.concurrent.Executors;

import org.jboss.as.model.ManagementElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.impl.deployment.ServerDeploymentManagerImpl;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.mgmt.ManagementCommunicationService;
import org.jboss.as.server.mgmt.ManagementCommunicationServiceInjector;
import org.jboss.as.server.mgmt.ServerControllerOperationHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

public class ManagementInstallationService implements Service<ManagementInstallationService>  {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "installer");

    private final InjectedValue<ServerController> serverControllerValue = new InjectedValue<ServerController>();

    public synchronized void start(StartContext context) throws StartException {
        ServiceTarget target = context.getController().getServiceContainer().subTarget();
        ServerModel serverModel = serverControllerValue.getValue().getServerModel();

        ManagementElement managementElement = serverModel.getManagementElement();
        if (managementElement != null) {
            System.out.println("Installing!");
            final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
            target.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
                    .addDependency(
                            NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(managementElement.getInterfaceName()),
                            NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
                    .addInjection(managementCommunicationService.getPortInjector(), managementElement.getPort())
                    .addInjection(managementCommunicationService.getExecutorServiceInjector(), Executors.newCachedThreadPool())
                    .addInjection(managementCommunicationService.getThreadFactoryInjector(), Executors.defaultThreadFactory())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            ServerControllerOperationHandler clientOperationHandler = new ServerControllerOperationHandler();
            target.addService(ServerControllerOperationHandler.SERVICE_NAME, clientOperationHandler)
                    .addDependency(Services.JBOSS_SERVER_CONTROLLER, ServerController.class,
                            clientOperationHandler.getServerControllerInjector())
                    .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class,
                            new ManagementCommunicationServiceInjector(clientOperationHandler))
                    .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class,
                            clientOperationHandler.getDeploymentRepositoryInjector())
                    .addDependency(ServerDeploymentManager.SERVICE_NAME_LOCAL, ServerDeploymentManager.class,
                            clientOperationHandler.getDeploymentManagerInjector())
                    // FIXME inject executor
                    .addInjection(clientOperationHandler.getExecutorValue(), Executors.newCachedThreadPool())
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            ServerDeploymentManagerImpl.addService(serverModel, context.getController().getServiceContainer(), target);
        }

    }

    /**
     * Stops the service.  Will shutdown the socket listener and will no longer accept requests.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
    }

    /** {@inheritDoc} */
    public ManagementInstallationService getValue() throws IllegalStateException {
        return this;
    }

    /**
     * Get the interface binding injector.
     *
     * @return The injector
     */
    public Injector<ServerController> getServerControllerInjector() {
        return serverControllerValue;
    }
}
