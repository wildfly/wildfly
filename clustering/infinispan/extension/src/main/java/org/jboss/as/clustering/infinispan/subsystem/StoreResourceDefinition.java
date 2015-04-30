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

import org.jboss.as.clustering.controller.AttributeMarshallers;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StoreResourceDefinition extends SimpleResourceDefinition {

    // attributes
    static final SimpleAttributeDefinition FETCH_STATE = new SimpleAttributeDefinitionBuilder(ModelKeys.FETCH_STATE, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.FETCH_STATE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition PASSIVATION = new SimpleAttributeDefinitionBuilder(ModelKeys.PASSIVATION, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.PASSIVATION.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition PRELOAD = new SimpleAttributeDefinitionBuilder(ModelKeys.PRELOAD, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.PRELOAD.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final SimpleAttributeDefinition PURGE = new SimpleAttributeDefinitionBuilder(ModelKeys.PURGE, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.PURGE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition SHARED = new SimpleAttributeDefinitionBuilder(ModelKeys.SHARED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.SHARED.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final SimpleAttributeDefinition SINGLETON = new SimpleAttributeDefinitionBuilder(ModelKeys.SINGLETON, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.SINGLETON.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final SimpleMapAttributeDefinition PROPERTIES = new SimpleMapAttributeDefinition.Builder(ModelKeys.PROPERTIES, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshallers.PROPERTY_LIST)
            .setDefaultValue(new ModelNode().setEmptyList())
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            SHARED, PRELOAD, PASSIVATION, FETCH_STATE, PURGE, SINGLETON, PROPERTIES
    };

    private final boolean allowRuntimeOnlyRegistration;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            OperationTransformer putPropertyTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(PROPERTIES.getName())) {
                        String key = operation.get("key").asString();
                        ModelNode value = Operations.getAttributeValue(operation);
                        PathAddress address = Operations.getPathAddress(operation);
                        ModelNode transformedOperation = Util.createAddOperation(address.append(StorePropertyResourceDefinition.pathElement(key)));
                        transformedOperation.get(StorePropertyResourceDefinition.VALUE.getName()).set(value);
                        return transformedOperation;
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(MapOperations.MAP_PUT_DEFINITION.getName(), new SimpleOperationTransformer(putPropertyTransformer));

            OperationTransformer removePropertyTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.get(ModelDescriptionConstants.NAME).asString().equals(PROPERTIES.getName())) {
                        String key = operation.get("key").asString();
                        PathAddress address = Operations.getPathAddress(operation);
                        return Util.createRemoveOperation(address.append(StorePropertyResourceDefinition.pathElement(key)));
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(MapOperations.MAP_PUT_DEFINITION.getName(), new SimpleOperationTransformer(removePropertyTransformer));
        }

        StorePropertyResourceDefinition.buildTransformation(version, builder);
        StoreWriteBehindResourceDefinition.buildTransformation(version, builder);
    }

    StoreResourceDefinition(StoreType type, boolean allowRuntimeOnlyRegistration) {
        super(type.pathElement(), type.getResourceDescriptionResolver(), type.getAddHandler(), type.getRemoveHandler());
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new StoreMetricExecutor(), StoreMetric.class).register(registration);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        // child resources
        registration.registerSubModel(new StoreWriteBehindResourceDefinition());
        registration.registerSubModel(new StorePropertyResourceDefinition());
    }
}
