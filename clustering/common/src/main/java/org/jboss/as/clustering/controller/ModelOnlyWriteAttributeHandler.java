/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.controller.transform.InitialAttributeValueOperationContextAttachment;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.TransformerOperationAttachment;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
public class ModelOnlyWriteAttributeHandler extends org.jboss.as.controller.ModelOnlyWriteAttributeHandler implements Registration<ManagementResourceRegistration> {

    private final WriteAttributeStepHandlerDescriptor descriptor;

    public ModelOnlyWriteAttributeHandler(WriteAttributeStepHandlerDescriptor descriptor) {
        super(descriptor.getAttributes().stream().toArray(AttributeDefinition[]::new));
        this.descriptor = descriptor;
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
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

    @Override
    public void register(ManagementResourceRegistration registration) {
        this.descriptor.getAttributes().forEach(attribute -> registration.registerReadWriteAttribute(attribute, null, this));
    }
}
