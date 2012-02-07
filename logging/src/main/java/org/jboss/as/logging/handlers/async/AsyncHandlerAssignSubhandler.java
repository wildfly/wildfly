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

package org.jboss.as.logging.handlers.async;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.logging.LoggingLogger;
import org.jboss.as.logging.loggers.AbstractLogHandlerAssignmentHandler;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.InjectedValue;

import java.util.List;
import java.util.logging.Handler;


/**
 * Operation responsible assigning a subhandler to an async handler.
 *
 * @author Stan Silvert
 */
public class AsyncHandlerAssignSubhandler extends AbstractLogHandlerAssignmentHandler {
    public static final String OPERATION_NAME = "assign-subhandler";
    public static final AsyncHandlerAssignSubhandler INSTANCE = new AsyncHandlerAssignSubhandler();

    @Override
    protected void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        NAME.validateAndSet(operation, model);
        updateHandlersForAssign(SUBHANDLERS, operation, model);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String asyncHandlerName = address.getLastElement().getValue();
        String handlerNameToAdd = NAME.resolveModelAttribute(context, model).asString();
        addHandler(context, asyncHandlerName, handlerNameToAdd);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, final ModelNode operation, final ModelNode model, List<ServiceController<?>> controllers) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String asyncHandlerName = address.getLastElement().getValue();
        String handlerName = model.get(NAME.getName()).asString();
        try {
            AsyncHandlerUnassignSubhandler.removeHandler(context, asyncHandlerName, handlerName);
        } catch (OperationFailedException e) {
            LoggingLogger.ROOT_LOGGER.errorRevertingOperation(e, getClass().getSimpleName(),
                    operation.require(ModelDescriptionConstants.OP).asString(),
                    PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)));
        }
    }

    @Override
    protected String getHandlerName(final ModelNode model) throws OperationFailedException {
        return NAME.validateOperation(model).asString();
    }

    /**
     * Adds the handler, represented by the {@code handlerNameToRemove} parameter, from the async handler.
     *
     * @param context          the context of the operation.
     * @param asyncHandlerName the async handler name.
     * @param handlerNameToAdd the name of the handler to add.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public static void addHandler(final OperationContext context, final String asyncHandlerName, final String handlerNameToAdd) throws OperationFailedException {
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        @SuppressWarnings("unchecked")
        final ServiceController<Handler> asyncHandlerController = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(asyncHandlerName));
        @SuppressWarnings("unchecked")
        final ServiceController<Handler> handlerToAssignController = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(handlerNameToAdd));

        if (handlerToAssignController == null) {
            throw createFailureMessage(MESSAGES.handlerNotFound(handlerNameToAdd));
        }

        final AsyncHandlerService service = AsyncHandlerService.class.cast(asyncHandlerController.getService());
        final InjectedValue<Handler> injectedHandler = new InjectedValue<Handler>();
        injectedHandler.inject(handlerToAssignController.getValue());

        service.addHandler(injectedHandler);
    }

    /**
     * Adds the subhandlers to the async handler.
     *
     * @param attribute the attribute definition.
     * @param node      the model node to extract the subhandlers from.
     * @param context   the context of the operation.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public static void addHandlers(final AttributeDefinition attribute, final ModelNode node, final OperationContext context,
                                   final String asyncHandlerName) throws OperationFailedException {
        final ModelNode handlers = attribute.resolveModelAttribute(context, node);
        if (handlers.isDefined()) {
            if (handlers.getType() == ModelType.LIST) {
                for (ModelNode handler : handlers.asList()) {
                    addHandler(context, asyncHandlerName, handler.asString());
                }
            }
        }
    }
}
