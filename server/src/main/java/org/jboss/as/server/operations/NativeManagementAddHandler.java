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

package org.jboss.as.server.operations;

import java.security.AccessController;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.server.Services;
import org.jboss.as.server.mgmt.ManagementCommunicationService;
import org.jboss.as.server.mgmt.ServerControllerOperationHandlerService;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;

/**
 * @author Emanuel Muckenhuber
 */
public class NativeManagementAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final NativeManagementAddHandler INSTANCE = new NativeManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();

        model.get(ModelDescriptionConstants.INTERFACE).set(interfaceName);
        model.get(ModelDescriptionConstants.PORT).set(port);
    }

    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();

        final ServiceTarget serviceTarget = context.getServiceTarget();

        Logger.getLogger("org.jboss.as").infof("creating native management service using network interface (%s) port (%s)", interfaceName, port);

        final ThreadGroup threadGroup = new ThreadGroup("ManagementCommunication-threads");
        final ThreadFactory threadFactory = new JBossThreadFactory(threadGroup, Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());

        final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
        newControllers.add(serviceTarget.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                        NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
                .addInjection(managementCommunicationService.getPortInjector(), port)
                .addInjection(managementCommunicationService.getExecutorServiceInjector(), Executors.newCachedThreadPool(threadFactory))
                .addInjection(managementCommunicationService.getThreadFactoryInjector(), threadFactory)
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());

        ServerControllerOperationHandlerService operationHandlerService = new ServerControllerOperationHandlerService();
        newControllers.add(serviceTarget.addService(ServerControllerOperationHandlerService.SERVICE_NAME, operationHandlerService)
                .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, operationHandlerService.getManagementCommunicationServiceValue())
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, operationHandlerService.getModelControllerValue())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ManagementDescription.getAddNativeManagementDescription(locale);
    }

}
