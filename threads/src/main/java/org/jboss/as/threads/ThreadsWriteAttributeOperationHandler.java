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

package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.WriteAttributeOperationHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * Abstract superclass for write-attribute operation handlers that run on the
 * server.
 *
 * @author Brian Stansberry
 * @author Alexey Loubyansky
 */
public abstract class ThreadsWriteAttributeOperationHandler extends WriteAttributeOperationHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.threads");

    private static final EnumSet<AttributeAccess.Flag> RESTART_NONE = EnumSet.of(AttributeAccess.Flag.RESTART_NONE);
    private static final EnumSet<AttributeAccess.Flag> RESTART_ALL = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);

    protected final Map<String, AttributeDefinition> attributes = new HashMap<String, AttributeDefinition>();
    protected final Map<String, AttributeDefinition> runtimeAttributes = new HashMap<String, AttributeDefinition>();

    /**
     * Creates a handler that doesn't validate values.
     */
    public ThreadsWriteAttributeOperationHandler(AttributeDefinition[] attrs, AttributeDefinition[] rwAttrs) {
        for(AttributeDefinition attr : attrs) {
            attributes.put(attr.getName(), attr);
        }
        for(AttributeDefinition attr : rwAttrs) {
            runtimeAttributes.put(attr.getName(), attr);
        }
    }

    @Override
    protected void modelChanged(final OperationContext context, final ModelNode operation,
                                final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

        boolean restartRequired = false;
        boolean applyToRuntime = context.getType() == OperationContext.Type.SERVER;
        ModelNode resolvedValue = null;
        if (applyToRuntime) {
            validateResolvedValue(attributeName, newValue);
            resolvedValue = newValue.resolve();
            restartRequired = applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue);
            if (restartRequired) {
                context.reloadRequired();
            }
        }

        if (context.completeStep() != OperationContext.ResultAction.KEEP && applyToRuntime) {
            ModelNode valueToRestore = currentValue.resolve();
            try {
                revertUpdateToRuntime(context, operation, attributeName, valueToRestore, resolvedValue);
            } catch (Exception e) {
                log.errorf(e, String.format("%s caught exception attempting to revert operation %s at address %s",
                        getClass().getSimpleName(),
                        operation.require(ModelDescriptionConstants.OP).asString(),
                        PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR))));
            }
            if (restartRequired) {
                context.revertReloadRequired();
            }
        }
    }

    /**
     * If a resolved value validator was passed to the constructor, uses it to validate the value.
     * Subclasses can alter this behavior.
     */
    protected void validateResolvedValue(String name, ModelNode value) throws OperationFailedException {
        AttributeDefinition attr = attributes.get(name);
        attr.getValidator().validateResolvedParameter(name, value);
    }


    /**
     * Hook to allow subclasses to make runtime changes to effect the attribute value change. Runtime changes
     * should be implemented by calling {@link org.jboss.as.controller.OperationContext#addStep(org.jboss.as.controller.OperationStepHandler, org.jboss.as.controller.OperationContext.Stage) adding a new step}
     * with {@link org.jboss.as.controller.OperationContext.Stage#RUNTIME}.
     * <p>
     * This default implementation simply returns {@code true}.
     * </p>
     *
     * @param context the context of the operation
     * @param operation the operation
     * @param attributeName the name of the attribute being modified
     * @param newValue the new value for the attribute
     * @param currentValue the existing value for the attribute
     *
     * @return {@code true} if the server requires restart to effect the attribute
     *         value change; {@code false} if not
     */
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {
        AttributeDefinition attr = runtimeAttributes.get(attributeName);
        if (attr == null) {
            // Not a runtime attribute; restart required
            return true;
        }
        else {
            final ServiceController<?> service = getService(context, operation);
            if (service == null) {
                // The service isn't installed, so the work done in the Stage.MODEL part is all there is to it
                return false;
            } else if (service.getState() != ServiceController.State.UP) {
                // Service is installed but not up?
                //throw new IllegalStateException(String.format("Cannot apply attribue %s to runtime; service %s is not in state %s, it is in state %s",
                //            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
                // No, don't barf; just let the update apply to the model and put the server in a reload-required state
                return true;
            } else {
                // Actually apply the update
                applyOperation(operation, attributeName, service);
                return false;
            }

        }
    }

    /**
     * Hook to allow subclasses to revert runtime changes made in
     * {@link #applyUpdateToRuntime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, String, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)}.
     * <p>
     * This default implementation simply does nothing.
     * </p>
     *
     * @param context the context of the operation
     * @param operation the operation
     * @param attributeName the name of the attribute being modified
     * @param valueToRestore the previous value for the attribute, before this operation was executed
     * @param valueToRevert the new value for the attribute that should be reverted
     */
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                         final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert) throws OperationFailedException {
        if (runtimeAttributes.containsKey(attributeName)) {
            final ServiceController<?> service = getService(context, operation);
            if (service != null && service.getState() == ServiceController.State.UP) {
                // Create and execute a write-attribute operation that uses the valueToRestore
                ModelNode revertOp = operation.clone();
                revertOp.get(attributeName).set(valueToRestore);
                applyOperation(revertOp, attributeName, service);
            }
        }
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for(String attrName : attributes.keySet()) {
            EnumSet<AttributeAccess.Flag> flags = runtimeAttributes.containsKey(attrName) ? RESTART_NONE : RESTART_ALL;
            registry.registerReadWriteAttribute(attrName, null, this, flags);
        }
    }

    @Override
    protected void validateValue(String name, ModelNode value)
            throws OperationFailedException {
                AttributeDefinition attr = attributes.get(name);
                attr.getValidator().validateParameter(name, value);
            }

    protected ServiceController<?> getService(final OperationContext context, final ModelNode operation) {
        final String name = Util.getNameFromAddress(operation.require(OP_ADDR));
        return context.getServiceRegistry(true).getService(ThreadsServices.threadFactoryName(name));
    }

    protected abstract void applyOperation(ModelNode operation, String attributeName, ServiceController<?> service);
}
