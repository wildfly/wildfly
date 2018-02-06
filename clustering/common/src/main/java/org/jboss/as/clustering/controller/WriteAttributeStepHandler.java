/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Map;
import java.util.function.Predicate;

import org.jboss.as.clustering.controller.transform.InitialAttributeValueOperationContextAttachment;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformerOperationAttachment;
import org.jboss.dmr.ModelNode;

/**
 * Convenience extension of {@link org.jboss.as.controller.ReloadRequiredWriteAttributeHandler} that can be initialized with an {@link Attribute} set.
 * @author Paul Ferraro
 */
public class WriteAttributeStepHandler extends ReloadRequiredWriteAttributeHandler implements Registration<ManagementResourceRegistration> {

    private final WriteAttributeStepHandlerDescriptor descriptor;
    private final ResourceServiceHandler handler;

    public WriteAttributeStepHandler(WriteAttributeStepHandlerDescriptor descriptor) {
        this(descriptor, null);
    }

    public WriteAttributeStepHandler(WriteAttributeStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
        super(descriptor.getAttributes());
        this.descriptor = descriptor;
        this.handler = handler;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return super.requiresRuntime(context) && (this.handler != null);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
            registration.registerReadWriteAttribute(attribute, null, this);
        }
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, AttributeDefinition attribute, ModelNode newValue, ModelNode oldValue) {
        Map<Capability, Predicate<ModelNode>> capabilities = this.descriptor.getCapabilities();
        if (!capabilities.isEmpty()) {
            PathAddress address = context.getCurrentAddress();
            // newValue is already applied to the model
            ModelNode newModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            ModelNode oldModel = newModel.clone();
            oldModel.get(attribute.getName()).set(oldValue);
            for (Map.Entry<Capability, Predicate<ModelNode>> entry : capabilities.entrySet()) {
                Capability capability = entry.getKey();
                Predicate<ModelNode> predicate = entry.getValue();
                boolean registered = predicate.test(oldModel);
                boolean shouldRegister = predicate.test(newModel);
                if (!registered && shouldRegister) {
                    // Attribute change enables capability registration
                    context.registerCapability(capability.resolve(address));
                } else if (registered && !shouldRegister) {
                    // Attribute change disables capability registration
                    context.deregisterCapability(capability.resolve(address).getName());
                }
            }
        }
        super.recordCapabilitiesAndRequirements(context, attribute, newValue, oldValue);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handback) throws OperationFailedException {
        boolean updated = super.applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, handback);
        if (updated) {
            PathAddress address = context.getCurrentAddress();
            if (context.isResourceServiceRestartAllowed() && this.getAttributeDefinition(attributeName).getFlags().contains(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES) && context.markResourceRestarted(address, this.handler)) {
                this.restartServices(context);
                // Returning false prevents going into reload required state
                return false;
            }
        }
        return updated;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode resolvedValue, Void handback) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        if (context.isResourceServiceRestartAllowed() && this.getAttributeDefinition(attributeName).getFlags().contains(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES) && context.revertResourceRestarted(address, this.handler)) {
            this.restartServices(context);
        }
    }

    private void restartServices(OperationContext context) throws OperationFailedException {
        this.handler.removeServices(context, context.getOriginalRootResource().navigate(context.getCurrentAddress()).getModel());
        this.handler.installServices(context, context.readResource(PathAddress.EMPTY_ADDRESS).getModel());
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);

        if (!context.isBooting()) {
            TransformerOperationAttachment attachment = TransformerOperationAttachment.getOrCreate(context);
            InitialAttributeValueOperationContextAttachment valuesAttachment = attachment.getAttachment(InitialAttributeValueOperationContextAttachment.INITIAL_VALUES_ATTACHMENT);
            if (valuesAttachment == null) {
                valuesAttachment = new InitialAttributeValueOperationContextAttachment();
                attachment.attach(InitialAttributeValueOperationContextAttachment.INITIAL_VALUES_ATTACHMENT, valuesAttachment);
            }
            valuesAttachment.putIfAbsentInitialValue(Operations.getPathAddress(operation), attributeName, oldValue);
        }
    }

}
