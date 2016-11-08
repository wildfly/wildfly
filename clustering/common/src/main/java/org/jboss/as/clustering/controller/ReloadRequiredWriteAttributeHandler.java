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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformerOperationAttachment;
import org.jboss.dmr.ModelNode;

/**
 * Convenience extension of {@link org.jboss.as.controller.ReloadRequiredWriteAttributeHandler} that can be initialized with an {@link Attribute} set.
 * @author Paul Ferraro
 */
public class ReloadRequiredWriteAttributeHandler extends org.jboss.as.controller.ReloadRequiredWriteAttributeHandler implements Registration<ManagementResourceRegistration> {

    private final WriteAttributeStepHandlerDescriptor descriptor;

    public ReloadRequiredWriteAttributeHandler(WriteAttributeStepHandlerDescriptor descriptor) {
        super(descriptor.getAttributes());
        this.descriptor = descriptor;
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        this.descriptor.getAttributes().forEach(attribute -> registration.registerReadWriteAttribute(attribute, null, this));
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
