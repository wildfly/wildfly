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

package org.jboss.as.controller.operations.global;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.operations.global.GlobalOperationAttributes.CHILD_TYPE;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} querying the children names of a given "child-type".
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadChildrenNamesHandler implements OperationStepHandler {

    static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_CHILDREN_NAMES_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(CHILD_TYPE)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .build();

    static final OperationStepHandler INSTANCE = new ReadChildrenNamesHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String childType = CHILD_TYPE.resolveModelAttribute(context, operation).asString();
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, false);
        ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        Map<String, Set<String>> childAddresses = GlobalOperationHandlers.getChildAddresses(context, address, registry, resource, childType);
        Set<String> childNames = childAddresses.get(childType);
        if (childNames == null) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownChildType(childType)));
        }
        // Sort the result
        childNames = new TreeSet<String>(childNames);
        ModelNode result = context.getResult();
        result.setEmptyList();
        PathAddress childAddress = address.append(PathElement.pathElement(childType));
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, childAddress);
        op.get(OPERATION_HEADERS).set(operation.get(OPERATION_HEADERS));
        ModelNode opAddr = op.get(OP_ADDR);
        ModelNode childProperty = opAddr.require(address.size());
        Set<Action.ActionEffect> actionEffects = EnumSet.of(Action.ActionEffect.ADDRESS);
        FilteredData fd = null;
        for (String childName : childNames) {
            childProperty.set(childType, new ModelNode(childName));
            if (context.authorize(op, actionEffects).getDecision() == AuthorizationResult.Decision.PERMIT) {
                result.add(childName);
            } else {
                if (fd == null) {
                    fd = new FilteredData(address);
                }
                fd.addAccessRestrictedResource(PathAddress.pathAddress(opAddr));
            }
        }

        if (fd != null) {
            context.getResponseHeaders().get(ACCESS_CONTROL).set(fd.toModelNode());
        }

        context.stepCompleted();
    }
}
