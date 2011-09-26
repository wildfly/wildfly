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
import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.ServiceController;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;
import org.jboss.dmr.ModelNode;


/**
 * Operation responsible unassigning a handler from a logger.
 *
 * @author Stan Silvert
 */
public class LoggerUnassignHandler extends AbstractModelUpdateHandler {
    static final String OPERATION_NAME = "unassign-handler";
    static final LoggerUnassignHandler INSTANCE = new LoggerUnassignHandler();

    protected String getHandlerName(ModelNode operation) throws OperationFailedException {
        return getHandlerNameNode(operation).asString();
    }

    protected ModelNode getHandlerNameNode(ModelNode operation) throws OperationFailedException {
        return NAME.validateResolvedOperation(operation);
    }

    protected String getLoggerName(ModelNode operation) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        return address.getLastElement().getValue();
    }

    protected void opFailed(String description) throws OperationFailedException {
        ModelNode failure = new ModelNode();
        failure.get(FAILURE_DESCRIPTION, description);
        throw new OperationFailedException(failure);
    }

    protected ModelNode getAssignedHandlers(ModelNode updateableModel) throws OperationFailedException {
        return updateableModel.get(HANDLERS);
    }

    /**
     * Get the ModelNode that has a "handlers" attribute.
     * @param model The root model for the operation.
     * @return The ModelNode that has a "handlers" attribute.
     */
    protected ModelNode getTargetModel(ModelNode model) throws OperationFailedException {
        return model;
    }

    @Override
    protected void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        String handlerName = getHandlerName(operation);
        ModelNode handlerNameNode = getHandlerNameNode(operation);

        ModelNode targetModel = getTargetModel(model);

        ModelNode assignedHandlers = getAssignedHandlers(targetModel);
        if (!assignedHandlers.isDefined() || !assignedHandlers.asList().contains(handlerNameNode))
            opFailed(MESSAGES.cannotUnassignHandler(handlerName));

        List<ModelNode> newList = new ArrayList<ModelNode>();
        for (ModelNode node : assignedHandlers.asList()) {
            if (node.asString().equals(handlerName)) continue;
            newList.add(node);
        }

        model.get(HANDLERS).set(newList);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        String loggerName = getLoggerName(operation);
        String handlerName = getHandlerName(operation);

        context.removeService(LogServices.loggerHandlerName(loggerName, handlerName));
    }

}
