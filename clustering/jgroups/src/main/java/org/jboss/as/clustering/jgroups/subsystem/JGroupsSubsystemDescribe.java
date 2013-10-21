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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * This handler is used to generate, given a JGroups model, a list of JGroups resource commands which can be used to recreate
 * the model on another host.
 *
 * We use a custom handler as the JGroups subsystem makes non-standard use of stack operations add-protocol,remove-protocol for
 * adding and removing protocol child resources.
 *
 * NB THis was required in order to maintain ordering of protocol layers.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsSubsystemDescribe implements OperationStepHandler {

    public static final JGroupsSubsystemDescribe INSTANCE = new JGroupsSubsystemDescribe();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode result = new ModelNode();
        PathAddress opAddress = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));

        AuthorizationResult authResult = context.authorize(operation, GenericSubsystemDescribeHandler.DESCRIBE_EFFECTS);
        if (authResult.getDecision() != AuthorizationResult.Decision.PERMIT) {
            throw ControllerMessages.MESSAGES.unauthorized(operation.require(OP).asString(),
                    opAddress, authResult.getExplanation());
        }

        final PathAddress rootAddress = PathAddress.pathAddress(opAddress.getLastElement());
        final ModelNode subModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        result.add(JGroupsSubsystemAdd.createOperation(rootAddress.toModelNode(), subModel));

        if (subModel.hasDefined(ModelKeys.STACK)) {
            for (final Property stack: subModel.get(ModelKeys.STACK).asPropertyList()) {
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
                    // no optional transport:add parameters will be present, so use attributes list
                    result.add(createOperation(transportAddress, transport, TransportResourceDefinition.TRANSPORT_ATTRIBUTES));
                    addProtocolPropertyCommands(transport, transportAddress, result);
                }
                // protocol=*
                if (stack.getValue().get(ModelKeys.PROTOCOL).isDefined()) {
                    for (Property protocol : ProtocolStackAdd.getOrderedProtocolPropertyList(stack.getValue())) {
                        // no optional transport:add parameters will be present, so use attributes list
                        result.add(createProtocolOperation(ProtocolResourceDefinition.PROTOCOL_ATTRIBUTES, stackAddress,
                                protocol.getValue()));
                        ModelNode protocolAddress = stackAddress.clone();
                        protocolAddress.add(ModelKeys.PROTOCOL, protocol.getName());
                        addProtocolPropertyCommands(protocol.getValue(), protocolAddress, result);
                    }
                }
                // relay=RELAY
                if (stack.getValue().get(ModelKeys.RELAY, ModelKeys.RELAY_NAME).isDefined()) {
                    ModelNode relay = stack.getValue().get(ModelKeys.RELAY, ModelKeys.RELAY_NAME);
                    ModelNode relayAddress = stackAddress.clone();
                    relayAddress.add(ModelKeys.RELAY, ModelKeys.RELAY_NAME);
                    result.add(createOperation(relayAddress, relay, RelayResource.ATTRIBUTES));
                    addProtocolPropertyCommands(relay, relayAddress, result);
                    // remote-site=*
                    if (relay.get(ModelKeys.REMOTE_SITE).isDefined()) {
                        for (final Property remoteSite: relay.get(ModelKeys.REMOTE_SITE).asPropertyList()) {
                            ModelNode remoteSiteAddress = relayAddress.clone().add(ModelKeys.REMOTE_SITE, remoteSite.getName());
                            // no optional transport:add parameters will be present, so use attributes list
                            result.add(createOperation(remoteSiteAddress, remoteSite.getValue(), RemoteSiteResource.ATTRIBUTES));
                        }
                    }
                }
            }
        }
        context.getResult().set(result);
        context.stepCompleted();
    }

    private void addProtocolPropertyCommands(ModelNode protocol, ModelNode address, ModelNode result) throws OperationFailedException {

        if (protocol.hasDefined(ModelKeys.PROPERTY)) {
            for (Property property : protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                ModelNode propertyAddress = address.clone().add(ModelKeys.PROPERTY, property.getName());
                result.add(createOperation(propertyAddress, property.getValue(), PropertyResourceDefinition.VALUE));
            }
        }
    }

    static ModelNode createOperation(ModelNode address, ModelNode existing,AttributeDefinition ... attributes) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        for (final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    static ModelNode createProtocolOperation(AttributeDefinition[] attributes, ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ModelKeys.ADD_PROTOCOL, address);
        for (final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }
}
