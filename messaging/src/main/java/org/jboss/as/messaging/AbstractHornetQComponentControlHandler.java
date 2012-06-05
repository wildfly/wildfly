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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.messaging.CommonAttributes.*;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.MessagingLogger.ROOT_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Locale;

import org.hornetq.api.core.management.HornetQComponentControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Base class for {@link org.jboss.as.controller.OperationStepHandler} implementations for handlers that interact
 * with an implementation of a {@link HornetQComponentControl} subinterface to perform their function.  This base class
 * handles a "start" and "stop" operation as well as a "read-attribute" call reading runtime attribute "started".
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractHornetQComponentControlHandler<T extends HornetQComponentControl> extends AbstractRuntimeOnlyHandler {

    private static final String STOP = "stop";

    private ParametersValidator readAttributeValidator = new ParametersValidator();

    protected AbstractHornetQComponentControlHandler() {
        readAttributeValidator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String operationName = operation.require(OP).asString();

        HornetQComponentControl control = null;
        boolean appliedToRuntime = false;
        Object handback = null;
        if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
            readAttributeValidator.validate(operation);
            final String name = operation.require(NAME).asString();
            if (STARTED.equals(name)) {
                control = getHornetQComponentControl(context, operation, false);
                context.getResult().set(control.isStarted());
            } else {
                handleReadAttribute(name, context, operation);
            }
        } else if (START.equals(operationName)) {
            control = getHornetQComponentControl(context, operation, true);
            try {
                control.start();
                appliedToRuntime = true;
                context.getResult();
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }

        } else if (STOP.equals(operationName)) {
            control = getHornetQComponentControl(context, operation, true);
            try {
                control.stop();
                appliedToRuntime = true;
                context.getResult();
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else {
            handback = handleOperation(operationName, context, operation);
            appliedToRuntime = handback != null;
        }

        if (context.completeStep() != OperationContext.ResultAction.KEEP && appliedToRuntime) {
            try {
                if (START.equals(operationName)) {
                    control.stop();
                } else if (STOP.equals(operationName)) {
                    control.start();
                } else {
                    handleRevertOperation(operationName, context, operation, handback);
                }
            } catch (Exception e) {
                ROOT_LOGGER.revertOperationFailed(e, getClass().getSimpleName(),
                        operation.require(ModelDescriptionConstants.OP).asString(),
                        PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)));
            }
        }
    }

    public void register(final ManagementResourceRegistration registry) {

        registry.registerReadOnlyAttribute(STARTED, this, AttributeAccess.Storage.RUNTIME);

        registry.registerOperationHandler(START, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, START, getDescriptionPrefix());
            }
        });

        registry.registerOperationHandler(STOP, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, STOP, getDescriptionPrefix());
            }
        });
    }

    /**
     * Gets the {@link HornetQComponentControl} implementation used by this handler.
     *
     * @param hqServer the HornetQServer installed in the runtime
     * @param address the address being invoked
     * @return the runtime HornetQ control object associated with the given address
     */
    protected abstract T getHornetQComponentControl(HornetQServer hqServer, PathAddress address);

    protected abstract String getDescriptionPrefix();

    /**
     * Hook to allow subclasses to handle read-attribute requests for attributes other than {@link CommonAttributes#STARTED}.
     * Implementations must not call {@link org.jboss.as.controller.OperationContext#completeStep()}.
     * <p>
     * This default implementation just throws the exception returned by {@link #unsupportedAttribute(String)}.
     * </p>
     *
     *
     * @param attributeName the name of the attribute
     * @param context the operation context
     * @param operation
     * @throws OperationFailedException
     */
    protected void handleReadAttribute(String attributeName, OperationContext context, ModelNode operation) throws OperationFailedException {
        unsupportedAttribute(attributeName);
    }

    /**
     * Hook to allow subclasses to handle operations other than {@code read-attribute}, {@code start} and
     * {@code stop}. Implementations must not call {@link org.jboss.as.controller.OperationContext#completeStep()}.
     * <p>
     * This default implementation just throws the exception returned by {@link #unsupportedOperation(String)}.
     * </p>
     *
     *
     * @param operationName the name of the operation
     * @param context the operation context
     * @param operation the operation
     *
     * @return an object that can be passed back in {@link #handleRevertOperation(String, org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, Object)}
     *         if the operation should be reverted. A value of {@code null} is an indication that no reversible
     *         modification was made
     * @throws OperationFailedException
     */
    protected Object handleOperation(String operationName, OperationContext context, ModelNode operation) throws OperationFailedException {
        unsupportedOperation(operationName);
        throw MESSAGES.unsupportedOperation(operationName);
    }

    /**
     * Hook to allow subclasses to handle revert changes made in
     * {@link #handleOperation(String, org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode)}.
     * <p>
     * This default implementation does nothing.
     * </p>
     *
     *
     * @param operationName the name of the operation
     * @param context the operation context
     * @param operation the operation
     */
    protected void handleRevertOperation(String operationName, OperationContext context, ModelNode operation, Object handback) {
    }

    /**
     * Return an ISE with a message saying support for the attribute was not properly implemented. This handler should
     * only be called if for a "read-attribute" operation if {@link #register(org.jboss.as.controller.registry.ManagementResourceRegistration)}
     * registers the attribute, so a handler then not recognizing the attribute name would be a bug and this method
     * returns an exception highlighting that bug.
     *
     * @param attributeName the name of the attribute
     * @throws IllegalStateException an exception with a message indicating a bug in this handler
     */
    protected final void unsupportedAttribute(final String attributeName) {
        // Bug
        throw MESSAGES.unsupportedAttribute(attributeName);
    }

    /**
     * Return an ISE with a message saying support for the operation was not properly implemented. This handler should
     * only be called if for a n operation if {@link #register(org.jboss.as.controller.registry.ManagementResourceRegistration)}
     * registers it as a handler, so a handler then not recognizing the operation name would be a bug and this method
     * returns an exception highlighting that bug.
     *
     * @param operationName the name of the attribute
     * @throws IllegalStateException an exception with a message indicating a bug in this handler
     */
    protected final void unsupportedOperation(final String operationName) {
        // Bug
        throw MESSAGES.unsupportedOperation(operationName);
    }

    /**
     * Gets the runtime HornetQ control object that can help service this request.
     *
     * @param context  the operation context
     * @param operation the operation
     * @param forWrite {@code true} if this operation will modify the runtime; {@code false} if not.
     * @return the control object
     * @throws OperationFailedException
     */
    protected final T getHornetQComponentControl(final OperationContext context, final ModelNode operation, final boolean forWrite) throws OperationFailedException {
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(forWrite).getService(hqServiceName);
        HornetQServer server = HornetQServer.class.cast(hqService.getValue());
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
         T control = getHornetQComponentControl(server, address);
         if (control == null) {
             throw new OperationFailedException(ControllerMessages.MESSAGES.noHandler(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(operation.require(OP_ADDR))));
         }
         return control;

    }
}
