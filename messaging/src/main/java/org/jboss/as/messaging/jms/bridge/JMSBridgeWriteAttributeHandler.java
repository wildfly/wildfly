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

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Write attribute handler for attributes that update a JMS bridge resource.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeWriteAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

    public static final JMSBridgeWriteAttributeHandler INSTANCE = new JMSBridgeWriteAttributeHandler();

    private final Map<String, AttributeDefinition> attributes = new HashMap<String, AttributeDefinition>();

    private JMSBridgeWriteAttributeHandler() {
        for (AttributeDefinition attr : JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES) {
            attributes.put(attr.getName(), attr);
        }
        for (AttributeDefinition attr : JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES) {
            attributes.put(attr.getName(), attr);
        }
        for (AttributeDefinition attr : JMSBridgeDefinition.JMS_TARGET_ATTRIBUTES) {
            attributes.put(attr.getName(), attr);
        }
    }

    @Override
    protected void modelChanged(final OperationContext context, final ModelNode operation, final String attributeName,
                                final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final AttributeDefinition attr = attributes.get(attributeName);
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                if(attr.hasAlternative(resource.getModel())) {
                    context.setRollbackOnly();
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.altAttributeAlreadyDefined(attributeName)));
                }
                context.completeStep();
            }
        }, OperationContext.Stage.VERIFY);

        context.reloadRequired();

        if (context.completeStep() != OperationContext.ResultAction.KEEP) {
            context.revertReloadRequired();
        }
    }

    @Override
    protected void validateValue(String name, ModelNode value) throws OperationFailedException {
        AttributeDefinition attr = attributes.get(name);
        attr.getValidator().validateParameter(name, value);
    }
}