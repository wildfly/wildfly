/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import java.util.List;
import java.util.logging.Handler;
import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.InjectedValue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;


/**
 * Operation responsible assigning a subhandler to an async handler.
 *
 * @author Stan Silvert
 */
public class AsyncHandlerAssignSubhandler extends AbstractModelUpdateHandler {
    private static final String OPERATION_NAME = "assign-subhandler";
    private static final AsyncHandlerAssignSubhandler INSTANCE = new AsyncHandlerAssignSubhandler();

    /**
     * @return the OPERATION_NAME
     */
    public static String getOperationName() {
        return OPERATION_NAME;
    }

    /**
     * @return the INSTANCE
     */
    public static AsyncHandlerAssignSubhandler getInstance() {
        return INSTANCE;
    }

    protected void opFailed(String description) throws OperationFailedException {
        ModelNode failure = new ModelNode();
        failure.get(FAILURE_DESCRIPTION, description);
        throw new OperationFailedException(failure);
    }

    @Override
    protected void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        String handlerName = operation.get(CommonAttributes.NAME).asString();
        ModelNode assignedHandlers = model.get(CommonAttributes.SUBHANDLERS);
        if (assignedHandlers.isDefined() && assignedHandlers.asList().contains(operation.get(CommonAttributes.NAME)))
            opFailed("Handler " + handlerName + " is already assigned.");
        assignedHandlers.add(handlerName);
    }

    @Override
    protected void performRuntime (final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String asyncHandlerName = address.getLastElement().getValue();
        String handlerNameToAssign = operation.get(CommonAttributes.NAME).asString();

        ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        ServiceController<Handler> asyncHandlerController = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(asyncHandlerName));
        ServiceController<Handler> handlerToAssignController = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(handlerNameToAssign));

        if (handlerToAssignController == null) {
            opFailed("Handler " + handlerNameToAssign + " not found.");
        }

        AsyncHandlerService service = (AsyncHandlerService)asyncHandlerController.getService();
        InjectedValue<Handler> injectedHandler = new InjectedValue<Handler>();
        injectedHandler.inject(handlerToAssignController.getValue());

        service.addHandler(injectedHandler);
    }
}
