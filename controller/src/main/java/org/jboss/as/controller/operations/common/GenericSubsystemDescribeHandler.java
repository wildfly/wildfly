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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.Set;

/**
 * A generic handler recursively creating add operations for a managed resource using it's
 * attributes as the request-parameters.
 *
 * @author Emanuel Muckenhuber
 */
public class GenericSubsystemDescribeHandler implements OperationStepHandler {

    public static final OperationStepHandler INSTANCE = new GenericSubsystemDescribeHandler();

    protected GenericSubsystemDescribeHandler() {
        //
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final ModelNode address = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement()).toModelNode();
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode result = context.getResult();
        describe(resource, address, result, context.getResourceRegistration());
    }

    protected void describe(final Resource resource, final ModelNode address, ModelNode result, final ImmutableManagementResourceRegistration registration) {
        if(registration.isRemote() || registration.isRuntimeOnly() || resource.isProxy() || resource.isRuntime()) {
            return;
        }
        final Set<PathElement> children = registration.getChildAddresses(PathAddress.EMPTY_ADDRESS);
        result.add(createAddOperation(address, resource.getModel(), children));
        for(final PathElement element : children) {
            if(element.isMultiTarget()) {
                final String childType = element.getKey();
                for(final Resource.ResourceEntry entry : resource.getChildren(childType)) {
                    final ImmutableManagementResourceRegistration childRegistration = registration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(childType, entry.getName())));
                    final ModelNode childAddress = address.clone();
                    childAddress.add(childType, entry.getName());
                    describe(entry, childAddress, result, childRegistration);
                }
            } else {
                final Resource child = resource.getChild(element);
                final ImmutableManagementResourceRegistration childRegistration = registration.getSubModel(PathAddress.pathAddress(element));
                final ModelNode childAddress = address.clone();
                childAddress.add(element.getKey(), element.getValue());
                describe(child, childAddress, result, childRegistration);
            }
        }
    }

    protected ModelNode createAddOperation(final ModelNode address, final ModelNode subModel, final Set<PathElement> children) {
        final ModelNode operation = subModel.clone();
        if(children != null && ! children.isEmpty()) {
            for(final PathElement path : children) {
                if(subModel.hasDefined(path.getKey())) {
                    subModel.remove(path.getKey());
                }
            }
        }
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        return operation;
    }

}
