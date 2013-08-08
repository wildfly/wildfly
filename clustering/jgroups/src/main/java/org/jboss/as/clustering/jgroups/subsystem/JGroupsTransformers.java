/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Collection point for all JGroups subsystem transformers.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class JGroupsTransformers {

    /**
     * Register the transformers for older model versions.
     *
     * @param subsystem the subsystems registration
     */
    public static void registerTransformers(final SubsystemRegistration subsystem) {
        registerTransformers_1_1_0(subsystem);
        registerTransformers_1_2_0(subsystem);
    }

    /*
     * Transformations from current to the 1.2.0 management model.
     *
     * We need to:
     * - remove any operations for relay=* and relay=*\/remote-site=* operations
     * - add the PROTOCOLS attribute back to the stack resource
     */
    private static void registerTransformers_1_2_0(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1, 2, 0);

        final TransformersSubRegistration registration = subsystem.registerModelTransformers(version, ResourceTransformer.DEFAULT);
        // reinstate PROTOCOLS on legacy stack models
        final TransformersSubRegistration stack = registration.registerSubResource(StackResourceDefinition.STACK_PATH, new AddProtocolsListToStackResourceTransformer());

        registerRelayTransformers(stack);
    }

    /*
     * Transformations from current to the 1.1.0 management model.
     *
     * We need to:
     * - reject expressions for transport (and similarly for protocol properties) for these operations
     *   transport=TRANSPORT/property=<name>:add(value=<value>)
     *   transport=TRANSPORT/property=<name>:write-attribute(name=value, value=<value>)
     *   transport=TRANSPORT:add(...,properties=<list of properties>)
     *
     * - remove any operations for relay=* and relay=*\/remote-site=* operations
     */
    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {

        final ModelVersion version = ModelVersion.create(1, 1, 0);
        final RejectExpressionValuesTransformer transformer = new RejectExpressionValuesTransformer(PropertyResourceDefinition.VALUE,
                TransportResourceDefinition.PROPERTIES, ProtocolResourceDefinition.PROPERTIES, TransportResourceDefinition.SHARED);
        final ResourceTransformer resourceTransformer = transformer;

        final TransformersSubRegistration registration = subsystem.registerModelTransformers(version, ResourceTransformer.DEFAULT);
        // reinstall PROTOCOLS on legacy stack models
        final TransformersSubRegistration stack = registration.registerSubResource(StackResourceDefinition.STACK_PATH, new AddProtocolsListToStackResourceTransformer());

        // reject expressions for transport properties, for the add and write-attribute op
        final TransformersSubRegistration transport = stack.registerSubResource(TransportResourceDefinition.TRANSPORT_PATH, resourceTransformer);
        transport.registerOperationTransformer(ADD, transformer);
        transport.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, transformer.getWriteAttributeTransformer());
        final TransformersSubRegistration transport_property = transport.registerSubResource(PropertyResourceDefinition.PROPERTY_PATH) ;
        transport_property.registerOperationTransformer(ADD, transformer);
        transport_property.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, transformer.getWriteAttributeTransformer());

        // reject expressions for transport properties, for the add and write-attribute op
        final TransformersSubRegistration protocol = stack.registerSubResource(ProtocolResourceDefinition.PROTOCOL_PATH, resourceTransformer);
        protocol.registerOperationTransformer(ADD, transformer);
        final TransformersSubRegistration protocol_property = protocol.registerSubResource(PropertyResourceDefinition.PROPERTY_PATH);
        protocol_property.registerOperationTransformer(ADD, transformer);
        protocol_property.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, transformer.getWriteAttributeTransformer());

        registerRelayTransformers(stack);
    }

    private static void registerRelayTransformers(final TransformersSubRegistration stack) {
        final TransformersSubRegistration relay = stack.registerSubResource(RelayResource.PATH, true);
        relay.discardOperations(ADD, WRITE_ATTRIBUTE_OPERATION);

        final TransformersSubRegistration remoteSite = relay.registerSubResource(RemoteSiteResource.PATH, true);
        remoteSite.discardOperations(ADD, WRITE_ATTRIBUTE_OPERATION);
    }

    private static class AddProtocolsListToStackResourceTransformer implements ResourceTransformer {

        private AddProtocolsListToStackResourceTransformer() {
        }

        @Override
        public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            if (resource.isProxy() || resource.isRuntime()) {
                return;
            }
            // perform stack=* specific transformation
            if (address.getLastElement().getKey().equals(ModelKeys.STACK)) {
                transformResourceInternal(context, address, resource) ;
            }

            ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
            childContext.processChildren(resource);
        }

        /*
         * Add back the PROTOCOLS list to the stack=* resource model.
         * It is a String LIST attribute holding the names of the protocols in the stack, in insertion order.
         */
        private void transformResourceInternal(final ResourceTransformationContext context, final PathAddress address, final Resource resource) throws OperationFailedException {
            ModelNode list = new ModelNode().setEmptyList();
            for (String name : resource.getChildrenNames(ModelKeys.PROTOCOL)) {
                list.add(name);
            }
            resource.getModel().get(ModelKeys.PROTOCOLS).set(list);
        }
    }
}
