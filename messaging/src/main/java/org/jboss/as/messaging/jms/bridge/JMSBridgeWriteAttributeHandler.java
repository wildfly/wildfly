/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms.bridge;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Write attribute handler for attributes that update a JMS bridge resource.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public static final JMSBridgeWriteAttributeHandler INSTANCE = new JMSBridgeWriteAttributeHandler();

    private JMSBridgeWriteAttributeHandler() {
        super(mergeAttributes());
    }

    private static AttributeDefinition[] mergeAttributes() {
        AttributeDefinition[] merged = new AttributeDefinition[JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES.length
                + JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES.length
                + JMSBridgeDefinition.JMS_TARGET_ATTRIBUTES.length];
        System.arraycopy(JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES, 0, merged, 0, JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES.length);
        System.arraycopy(JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES, 0, merged,
                JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES.length, JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES.length);
        System.arraycopy(JMSBridgeDefinition.JMS_TARGET_ATTRIBUTES, 0, merged,
                JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES.length + JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES.length,
                JMSBridgeDefinition.JMS_TARGET_ATTRIBUTES.length);
        return merged;
    }

    @Override
    protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final String attributeName = operation.require(NAME).asString();
                final AttributeDefinition attr = getAttributeDefinition(attributeName);
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                if(attr.hasAlternative(resource.getModel())) {
                    context.setRollbackOnly();
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.altAttributeAlreadyDefined(attributeName)));
                }
                context.stepCompleted();
            }
        }, OperationContext.Stage.VERIFY);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        return true;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        // no-op
    }
}
