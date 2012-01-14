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

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Abstract superclass for write-attribute operation handlers for the threads subsystem.
 *
 * @author Brian Stansberry
 * @author Alexey Loubyansky
 */
public abstract class ThreadsWriteAttributeOperationHandler extends AbstractWriteAttributeHandler<Boolean> {

    protected final AttributeDefinition[] attributes;
    protected final Map<String, AttributeDefinition> runtimeAttributes = new HashMap<String, AttributeDefinition>();

    /**
     * Creates a handler that doesn't validate values.
     * @param attributes all persistent attributes of the
     * @param runtimeAttributes attributes whose updated value can immediately be applied to the runtime
     */
    public ThreadsWriteAttributeOperationHandler(AttributeDefinition[] attributes, AttributeDefinition[] runtimeAttributes) {
        super(attributes);
        this.attributes = attributes;
        for(AttributeDefinition attr : runtimeAttributes) {
            this.runtimeAttributes.put(attr.getName(), attr);
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String attributeName, final ModelNode newValue,
                                           final ModelNode currentValue, final HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {
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
                final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                applyOperation(context, model, attributeName, service);
                handbackHolder.setHandback(Boolean.TRUE);
                return false;
            }

        }
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                         final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert, final Boolean handback) throws OperationFailedException {
        if (handback != null && handback.booleanValue() && runtimeAttributes.containsKey(attributeName)) {
            final ServiceController<?> service = getService(context, operation);
            if (service != null && service.getState() == ServiceController.State.UP) {
                // Create and execute a write-attribute operation that uses the valueToRestore
                ModelNode revertModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
                revertModel.get(attributeName).set(valueToRestore);
                applyOperation(context, revertModel, attributeName, service);
            }
        }
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for(AttributeDefinition attribute : attributes) {
            registry.registerReadWriteAttribute(attribute, null, this);
        }
    }

    protected abstract ServiceController<?> getService(final OperationContext context, final ModelNode model) throws OperationFailedException;

    protected abstract void applyOperation(final OperationContext context, ModelNode operation, String attributeName, ServiceController<?> service) throws OperationFailedException;
}
