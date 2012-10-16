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
package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROBLEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Iterator;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author Alexey Loubyansky
 */
public class ValidateAddressOperationHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "validate-address";
    public static final ValidateAddressOperationHandler INSTANCE = new ValidateAddressOperationHandler();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ControllerResolver.getResolver("global"))
        .addParameter(
                SimpleAttributeDefinitionBuilder.create(VALUE, ModelType.OBJECT)
                    .build())
        .setReplyParameters(
                SimpleAttributeDefinitionBuilder.create(VALID, ModelType.BOOLEAN).build(),
                SimpleAttributeDefinitionBuilder.create(PROBLEM, ModelType.STRING)
                    .setAllowNull(true)
                    .build()
                )
        .setReadOnly()
        .build();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode address = operation.require(VALUE);
        final PathAddress pathAddr = PathAddress.pathAddress(address);
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Resource model = resource;
        final Iterator<PathElement> iterator = pathAddr.iterator();
        int index = 0;
        out: while(iterator.hasNext()) {
            final PathElement next = iterator.next();
            index++;
            if(model.hasChild(next)) {
                model = model.getChild(next);
            } else {
                final PathAddress subAddress = pathAddr.subAddress(0, index);
                final ImmutableManagementResourceRegistration registration = context.getResourceRegistration().getSubModel(subAddress);

                if(registration != null) {
                    // If the target is a registered proxy return immediately
                    boolean remote = registration.isRemote();
                    if(remote && ! iterator.hasNext()) {
                        break out;
                    }
                    // Create the proxy op
                    final PathAddress newAddress = pathAddr.subAddress(index);
                    final ModelNode newOperation = operation.clone();
                    newOperation.get(OP_ADDR).set(subAddress.toModelNode());
                    newOperation.get(VALUE).set(newAddress.toModelNode());

                    // On the DC the host=master is not a proxy but the validate-address is registered at the root
                    // Otherwise delegate to the proxy handler
                    final OperationStepHandler proxyHandler = registration.getOperationHandler(PathAddress.EMPTY_ADDRESS, OPERATION_NAME);
                    if(proxyHandler != null) {
                        context.addStep(newOperation, proxyHandler, OperationContext.Stage.IMMEDIATE);
                        context.stepCompleted();
                        return;
                    }
                }

                // Invalid
                context.getResult().get(VALID).set(false);
                context.getResult().get(PROBLEM).set(ControllerMessages.MESSAGES.childResourceNotFound(next));
                context.stepCompleted();
                return;
            }
        }
        context.getResult().get(VALID).set(true);
        context.stepCompleted();
    }
}
