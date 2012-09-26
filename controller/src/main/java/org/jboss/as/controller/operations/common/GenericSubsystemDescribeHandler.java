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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A generic handler recursively creating add operations for a managed resource using it's
 * attributes as the request-parameters.
 *
 * @author Emanuel Muckenhuber
 */
public class GenericSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {

    public static final GenericSubsystemDescribeHandler INSTANCE = new GenericSubsystemDescribeHandler();
    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(DESCRIBE, ControllerResolver.getResolver(SUBSYSTEM))
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.OBJECT)
            .setPrivateEntry()
            .build();

    protected GenericSubsystemDescribeHandler() {
        //
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final ModelNode address;
        final PathAddress pa = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
        if (pa.size() > 0) {
            address = new ModelNode().add(pa.getLastElement().getKey(), pa.getLastElement().getValue());
        } else {
            address = new ModelNode().setEmptyList();
        }
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode result = context.getResult();
        describe(resource, address, result, context.getResourceRegistration());
        context.stepCompleted();
    }

    protected void describe(final Resource resource, final ModelNode address, ModelNode result, final ImmutableManagementResourceRegistration registration) {
        if(resource == null || registration.isRemote() || registration.isRuntimeOnly() || resource.isProxy() || resource.isRuntime() || registration.isAlias()) {
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
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        if(children != null && ! children.isEmpty()) {
            for(final PathElement path : children) {
                if(subModel.hasDefined(path.getKey())) {
                    subModel.remove(path.getKey());
                }
            }
        }
        return operation;
    }

    /**
     *
     * @param locale the locale to use to generate any localized text used in the description.
     *               May be {@code null}, in which case {@link Locale#getDefault()} should be used
     *
     * @return definition of operation
     * @deprecated use {@link #DEFINITION} for registration of operation
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        // This is a private operation, so we should not be getting requests for descriptions
        return DEFINITION.getDescriptionProvider().getModelDescription(locale);
    }
}
