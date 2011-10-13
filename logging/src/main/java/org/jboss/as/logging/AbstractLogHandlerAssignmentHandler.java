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

import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractLogHandlerAssignmentHandler extends AbstractModelUpdateHandler {

    /**
     * Updates the handlers to be assigned. Checks to see if the handler already exists and throws an
     * {@link OperationFailedException exception} if it does.
     *
     * @param handlerAttribute the handler attribute.
     * @param operation        the operation.
     * @param model            the new model
     *
     * @throws OperationFailedException if an error occurs.
     */
    protected void updateHandlersForAssign(final AttributeDefinition handlerAttribute, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String handlerName = getHandlerName(operation);
        if (handlerExists(handlerName, handlerAttribute, operation)) {
            throw createFailureMessage(MESSAGES.handlerAlreadyDefined(handlerName));
        }
        getAssignedHandlers(operation).add(handlerName);
        handlerAttribute.validateAndSet(operation, model);
    }

    /**
     * Updates the handlers to be unassigned. Checks to see if the handler exists and creates a new list of handlers
     * removing the handler to be unassigned. If the handler to be unassigned does not exist an
     * {@link OperationFailedException exception} is thrown.
     *
     * @param handlerAttribute the handler attribute.
     * @param operation        the operation.
     * @param model            the new model
     *
     * @throws OperationFailedException if an error occurs.
     */
    protected void updateHandlersForUnassign(final AttributeDefinition handlerAttribute, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String handlerName = getHandlerName(operation);
        if (handlerExists(handlerName, handlerAttribute, operation)) {
            // Get the current subhandlers
            final ModelNode currentSubhandlers = getAssignedHandlers(model);
            // Create new list of subhandlers without the handler being removed
            final List<ModelNode> newSubhandlers = new ArrayList<ModelNode>();
            for (ModelNode node : currentSubhandlers.asList()) {
                if (node.asString().equals(handlerName)) {
                    continue;
                }
                newSubhandlers.add(node);
            }
            // Replace the current handlers with the new handlers
            model.get(handlerAttribute.getName()).set(newSubhandlers);
        } else {
            // Subhandler not found
            throw createFailureMessage(MESSAGES.cannotUnassignHandler(handlerName));
        }
    }

    /**
     * Creates a {@link OperationFailedException failure exception} for the description.
     *
     * @param description the description for the exception.
     *
     * @return the exception.
     */
    protected OperationFailedException createFailureMessage(final String description) {
        ModelNode failure = new ModelNode();
        failure.get(FAILURE_DESCRIPTION, description);
        return new OperationFailedException(failure);
    }

    /**
     * Checks the {@link ModelNode model}, represented my the model parameter, to see if the handler already exists.
     *
     * @param handlerName      the name of the handler.
     * @param handlerAttribute the handler attribute definition.
     * @param model            the model.
     *
     * @return {@code true} if the handler already exists in the handler listing.
     *
     * @throws OperationFailedException if an operation failure occurs.
     */
    protected boolean handlerExists(final String handlerName, final AttributeDefinition handlerAttribute, final ModelNode model) throws OperationFailedException {
        final ModelNode assignedHandlers = model.get(handlerAttribute.getName());
        return (assignedHandlers.isDefined() && assignedHandlers.asList().contains(model.get(handlerName)));
    }

    /**
     * Returns the name of the handler.
     *
     * @param model the model node to retrieve the name from.
     *
     * @return the handler name.
     *
     * @throws OperationFailedException if a failure occurs.
     */
    protected abstract String getHandlerName(ModelNode model) throws OperationFailedException;

    /**
     * Returns the model node of the handlers that are already assigned.
     *
     * @param model the model that contains the handlers.
     *
     * @return the currently installed handlers.
     *
     * @throws OperationFailedException if a failure occurs.
     */
    protected abstract ModelNode getAssignedHandlers(ModelNode model) throws OperationFailedException;
}
