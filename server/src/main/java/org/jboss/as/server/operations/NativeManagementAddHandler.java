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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;
import java.util.concurrent.Executors;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.Services;
import org.jboss.as.server.mgmt.ManagementCommunicationService;
import org.jboss.as.server.mgmt.ServerControllerOperationHandlerService;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
public class NativeManagementAddHandler implements ModelAddOperationHandler, DescriptionProvider {

    public static final NativeManagementAddHandler INSTANCE = new NativeManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(ModelDescriptionConstants.REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();

        final ModelNode subModel = context.getSubModel();
        subModel.get(ModelDescriptionConstants.INTERFACE).set(interfaceName);
        subModel.get(ModelDescriptionConstants.PORT).set(port);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();

                    Logger.getLogger("org.jboss.as").infof("creating native management service using network interface (%s) port (%s)", interfaceName, port);

                    final ManagementCommunicationService managementCommunicationService = new ManagementCommunicationService();
                    serviceTarget.addService(ManagementCommunicationService.SERVICE_NAME, managementCommunicationService)
                            .addDependency(
                                    NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                                    NetworkInterfaceBinding.class, managementCommunicationService.getInterfaceInjector())
                            .addInjection(managementCommunicationService.getPortInjector(), port)
                            .addInjection(managementCommunicationService.getExecutorServiceInjector(), Executors.newCachedThreadPool())
                            .addInjection(managementCommunicationService.getThreadFactoryInjector(), Executors.defaultThreadFactory())
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();

                    ServerControllerOperationHandlerService operationHandlerService = new ServerControllerOperationHandlerService();
                    serviceTarget.addService(ServerControllerOperationHandlerService.SERVICE_NAME, operationHandlerService)
                            .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, operationHandlerService.getManagementCommunicationServiceValue())
                            .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, operationHandlerService.getModelControllerValue())
                            .setInitialMode(ServiceController.Mode.ACTIVE)
                            .install();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO Auto-generated method stub
        return new ModelNode();
    }

}
