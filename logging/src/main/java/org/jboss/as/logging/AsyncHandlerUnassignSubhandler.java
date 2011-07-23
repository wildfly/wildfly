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

import java.util.ArrayList;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;


/**
 * Operation responsible unassigning a handler from a logger.
 *
 * @author Stan Silvert
 */
public class AsyncHandlerUnassignSubhandler extends AbstractModelUpdateHandler {
    private static final String OPERATION_NAME = "unassign-subhandler";
    private static final AsyncHandlerUnassignSubhandler INSTANCE = new AsyncHandlerUnassignSubhandler();

    /**
     * @return the OPERATION_NAME
     */
    public static String getOperationName() {
        return OPERATION_NAME;
    }

    /**
     * @return the INSTANCE
     */
    public static AsyncHandlerUnassignSubhandler getInstance() {
        return INSTANCE;
    }

    protected void opFailed(String description) throws OperationFailedException {
        ModelNode failure = new ModelNode();
        failure.get(FAILURE_DESCRIPTION, description);
        throw new OperationFailedException(failure);
    }

    @Override
    protected void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode handlerNameNode = operation.get(CommonAttributes.NAME);
        String handlerName = handlerNameNode.asString();

        ModelNode assignedHandlers = model.get(CommonAttributes.SUBHANDLERS);
        if (!assignedHandlers.isDefined() || !assignedHandlers.asList().contains(handlerNameNode)) {
            opFailed("Can not unassign handler.  Handler " + handlerName + " is not assigned.");
        }

        List<ModelNode> newList = new ArrayList<ModelNode>();
        for (ModelNode node : assignedHandlers.asList()) {
            if (node.asString().equals(handlerName)) continue;
            newList.add(node);
        }

        model.get(CommonAttributes.SUBHANDLERS).set(newList);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String asyncHandlerName = address.getLastElement().getValue();
        String handlerNameToUnassign = operation.get(CommonAttributes.NAME).asString();

        ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        ServiceController<Handler> asyncHandlerController = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(asyncHandlerName));
        ServiceController<Handler> handlerToUnassignController = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(handlerNameToUnassign));

        AsyncHandlerService service = (AsyncHandlerService)asyncHandlerController.getService();
        Handler injectedHandler = handlerToUnassignController.getService().getValue();
        service.removeHandler(injectedHandler);
    }

}
