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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;
import java.util.concurrent.Executors;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.remote.ModelControllerOperationHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.Services;
import org.jboss.as.server.mgmt.ManagementCommunicationService;
import org.jboss.as.server.mgmt.ManagementCommunicationServiceInjector;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
public class ManagementSocketAddHandler implements ModelUpdateOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-management-socket";

    public static final ManagementSocketAddHandler INSTANCE = new ManagementSocketAddHandler();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set("remove-management-socket");
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceTarget serviceTarget = runtimeContext.getServiceTarget();

            Logger.getLogger("org.jboss.as").infof("creating management service using network interface (%s) port (%s)", interfaceName, port);

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

            ModelControllerOperationHandler modelControllerOperationHandler = new ModelControllerOperationHandler();
            serviceTarget.addService(Services.JBOSS_SERVER_CONTROLLER.append(ModelControllerOperationHandler.OPERATION_HANDLER_NAME_SUFFIX), modelControllerOperationHandler)
                    .addDependency(ManagementCommunicationService.SERVICE_NAME, ManagementCommunicationService.class, new ManagementCommunicationServiceInjector(modelControllerOperationHandler))
                    .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, modelControllerOperationHandler.getModelControllerValue())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(ModelDescriptionConstants.MANAGEMENT, ModelDescriptionConstants.INTERFACE).set(interfaceName);
        subModel.get(ModelDescriptionConstants.MANAGEMENT, ModelDescriptionConstants.PORT).set(port);

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO Auto-generated method stub
        return new ModelNode();
    }

}
