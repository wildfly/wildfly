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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Paul Ferraro
 */
public class JGroupsSubsystemDescribe implements OperationStepHandler {

    public static final JGroupsSubsystemDescribe INSTANCE = new JGroupsSubsystemDescribe();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode result = new ModelNode();
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement());
        final ModelNode subModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        result.add(JGroupsSubsystemAdd.createOperation(rootAddress.toModelNode(), subModel));

        if (subModel.hasDefined(ModelKeys.STACK)) {
            for (final Property stack : subModel.get(ModelKeys.STACK).asPropertyList()) {
                // process one stack
                final ModelNode stackAddress = rootAddress.toModelNode();
                stackAddress.add(ModelKeys.STACK, stack.getName());
                result.add(ProtocolStackAdd.createOperation(stackAddress, stack.getValue()));

                // process the child resources

                // transport=TRANSPORT
                if (stack.getValue().get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME).isDefined()) {
                    ModelNode transport = stack.getValue().get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
                    ModelNode transportAddress = stackAddress.clone();
                    transportAddress.add(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
                    result.add(StackConfigOperationHandlers.createOperation(CommonAttributes.TRANSPORT_ATTRIBUTES, transportAddress, transport));
                    addProtocolPropertyCommands(transport, transportAddress, result);
                }
                // protocol=*
                if (stack.getValue().get(ModelKeys.PROTOCOL).isDefined()) {
                    for (Property protocol : ProtocolStackAdd.getOrderedProtocolPropertyList(stack.getValue())) {
                        result.add(StackConfigOperationHandlers.createProtocolOperation(CommonAttributes.PROTOCOL_ATTRIBUTES, stackAddress, protocol.getValue()));
                        ModelNode protocolAddress = stackAddress.clone();
                        protocolAddress.add(ModelKeys.PROTOCOL, protocol.getName()) ;
                        addProtocolPropertyCommands(protocol.getValue(), protocolAddress, result);
                    }
                }
            }
        }
        context.getResult().set(result);
        context.completeStep();
    }

    private void addProtocolPropertyCommands(ModelNode protocol, ModelNode address, ModelNode result) throws OperationFailedException {

        if (protocol.hasDefined(ModelKeys.PROPERTY)) {
             for (Property property : protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                 ModelNode propertyAddress = address.clone().add(ModelKeys.PROPERTY, property.getName());
                 AttributeDefinition[] ATTRIBUTE = {CommonAttributes.VALUE} ;
                 result.add(StackConfigOperationHandlers.createOperation(ATTRIBUTE, propertyAddress, property.getValue()));
             }
        }
    }

}
