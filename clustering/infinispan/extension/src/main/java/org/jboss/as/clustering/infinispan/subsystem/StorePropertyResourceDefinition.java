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

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/eviction=EVICTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StorePropertyResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.PROPERTY, name);
    }

    // attributes
    static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelKeys.VALUE, ModelType.STRING, false)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Do nothing
    }

    StorePropertyResourceDefinition() {
        super(WILDCARD_PATH, new InfinispanResourceDescriptionResolver(ModelKeys.PROPERTY));
        this.setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion());
    }

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { VALUE };

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        OperationStepHandler readHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                PathAddress address = context.getCurrentAddress().getParent();
                String key = context.getCurrentAddressValue();
                ModelNode getOperation = Operations.createMapGetOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key);
                context.addStep(getOperation, MapOperations.MAP_GET_HANDLER, context.getCurrentStage());
            }
        };
        OperationStepHandler writeHandler = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                PathAddress address = context.getCurrentAddress().getParent();
                String key = context.getCurrentAddressValue();
                String value = Operations.getAttributeValue(operation).asString();
                ModelNode putOperation = Operations.createMapPutOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key, value);
                context.addStep(putOperation, MapOperations.MAP_PUT_HANDLER, context.getCurrentStage());
            }
        };
        registration.registerReadWriteAttribute(VALUE, readHandler, writeHandler);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        AbstractAddStepHandler addHandler = new AbstractAddStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                context.createResource(PathAddress.EMPTY_ADDRESS);
                String name = context.getCurrentAddressValue();
                String value = operation.get(VALUE.getName()).asString();
                PathAddress storeAddress = context.getCurrentAddress().getParent();
                ModelNode putOperation = Operations.createMapPutOperation(storeAddress, StoreResourceDefinition.PROPERTIES.getName(), name, value);
                context.addStep(putOperation, MapOperations.MAP_PUT_HANDLER, context.getCurrentStage());
            }
        };
        this.registerAddOperation(registration, addHandler);

        AbstractRemoveStepHandler removeHandler = new AbstractRemoveStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) {
                context.removeResource(PathAddress.EMPTY_ADDRESS);
                String name = context.getCurrentAddressValue();
                PathAddress storeAddress = context.getCurrentAddress().getParent();
                ModelNode putOperation = Operations.createMapRemoveOperation(storeAddress, StoreResourceDefinition.PROPERTIES.getName(), name);
                context.addStep(putOperation, MapOperations.MAP_REMOVE_HANDLER, context.getCurrentStage());
            }
        };
        this.registerRemoveOperation(registration, removeHandler);
    }
}
