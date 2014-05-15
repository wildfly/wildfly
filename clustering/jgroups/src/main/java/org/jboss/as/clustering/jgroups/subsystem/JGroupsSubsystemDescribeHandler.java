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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.logging.ControllerLogger;
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
public class JGroupsSubsystemDescribeHandler implements OperationStepHandler {

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode result = new ModelNode();
        PathAddress opAddress = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));

        AuthorizationResult authResult = context.authorize(operation, GenericSubsystemDescribeHandler.DESCRIBE_EFFECTS);
        if (authResult.getDecision() != AuthorizationResult.Decision.PERMIT) {
            throw ControllerLogger.ROOT_LOGGER.unauthorized(operation.require(OP).asString(), opAddress, authResult.getExplanation());
        }

        final PathAddress rootAddress = PathAddress.pathAddress(opAddress.getLastElement());
        final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        result.add(createOperation(rootAddress, model, JGroupsSubsystemResourceDefinition.ATTRIBUTES));

        if (model.hasDefined(StackResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property: model.get(StackResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                PathAddress stackAddress = rootAddress.append(StackResourceDefinition.pathElement(property.getName()));
                ModelNode stack = property.getValue();
                result.add(createOperation(stackAddress, stack));

                if (stack.get(TransportResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
                    ModelNode transport = stack.get(TransportResourceDefinition.PATH.getKeyValuePair());
                    PathAddress transportAddress = stackAddress.append(TransportResourceDefinition.PATH);
                    // no optional transport:add parameters will be present, so use attributes list
                    result.add(createOperation(transportAddress, transport, TransportResourceDefinition.ATTRIBUTES));
                    addProtocolPropertyCommands(transportAddress, transport, result);
                }
                if (stack.get(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).isDefined()) {
                    for (Property protocol : StackAddHandler.getOrderedProtocolPropertyList(stack)) {
                        // no optional transport:add parameters will be present, so use attributes list
                        result.add(createProtocolOperation(stackAddress, protocol.getValue(), ProtocolResourceDefinition.ATTRIBUTES));
                        PathAddress protocolAddress = stackAddress.append(ProtocolResourceDefinition.pathElement(protocol.getName()));
                        addProtocolPropertyCommands(protocolAddress, protocol.getValue(), result);
                    }
                }
                if (stack.get(RelayResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
                    ModelNode relay = stack.get(RelayResourceDefinition.PATH.getKeyValuePair());
                    PathAddress relayAddress = stackAddress.append(RelayResourceDefinition.PATH);
                    result.add(createOperation(relayAddress, relay, RelayResourceDefinition.ATTRIBUTES));
                    addProtocolPropertyCommands(relayAddress, relay, result);

                    if (relay.get(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey()).isDefined()) {
                        for (Property remoteSite: relay.get(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            PathAddress remoteSiteAddress = relayAddress.append(RemoteSiteResourceDefinition.pathElement(remoteSite.getName()));
                            // no optional transport:add parameters will be present, so use attributes list
                            result.add(createOperation(remoteSiteAddress, remoteSite.getValue(), RemoteSiteResourceDefinition.ATTRIBUTES));
                        }
                    }
                }
            }
        }
        context.getResult().set(result);
        context.stepCompleted();
    }

    private static void addProtocolPropertyCommands(PathAddress address, ModelNode protocol, ModelNode result) throws OperationFailedException {
        if (protocol.hasDefined(PropertyResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property : protocol.get(PropertyResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                PathAddress propertyAddress = address.append(PropertyResourceDefinition.pathElement(property.getName()));
                result.add(createOperation(propertyAddress, property.getValue(), PropertyResourceDefinition.VALUE));
            }
        }
    }

    private static ModelNode createOperation(PathAddress address, ModelNode existing, AttributeDefinition ... attributes) throws OperationFailedException {
        ModelNode operation = Util.createAddOperation(address);
        for (final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }

    private static ModelNode createProtocolOperation(PathAddress address, ModelNode existing, AttributeDefinition... attributes) throws OperationFailedException {
        ModelNode operation = Util.createOperation(ModelKeys.ADD_PROTOCOL, address);
        for (final AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(existing, operation);
        }
        return operation;
    }
}
