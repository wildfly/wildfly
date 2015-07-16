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
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
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
 * @author Paul Ferraro
 */
public abstract class StoreResourceDefinition extends SimpleResourceDefinition implements Registration {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("store", value);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        FETCH_STATE("fetch-state", true),
        PASSIVATION("passivation", true),
        PRELOAD("preload", false),
        PURGE("purge", true),
        SHARED("shared", false),
        SINGLETON("singleton", false),
        PROPERTIES("properties"),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, boolean defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.BOOLEAN)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(new ModelNode(defaultValue))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        Attribute(String name) {
            this.definition = new SimpleMapAttributeDefinition.Builder(name, true)
                    .setAllowExpression(true)
                    .setAttributeMarshaller(AttributeMarshallers.PROPERTY_LIST)
                    .setAttributeParser(AttributeParsers.COLLECTION)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final boolean allowRuntimeOnlyRegistration;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.discardChildResource(StoreWriteThroughResourceDefinition.PATH);
        } else {
            StoreWriteThroughResourceDefinition.buildTransformation(version, builder);
        }

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            OperationTransformer putPropertyTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    String attributeName = Operations.getAttributeName(operation);
                    if (Attribute.PROPERTIES.getDefinition().getName().equals(attributeName)) {
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
                    String attributeName = Operations.getAttributeName(operation);
                    if (Attribute.PROPERTIES.getDefinition().getName().equals(attributeName)) {
                        String key = operation.get("key").asString();
                        PathAddress address = Operations.getPathAddress(operation);
                        return Util.createRemoveOperation(address.append(StorePropertyResourceDefinition.pathElement(key)));
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(MapOperations.MAP_PUT_DEFINITION.getName(), new SimpleOperationTransformer(removePropertyTransformer));
        }

        StoreWriteBehindResourceDefinition.buildTransformation(version, builder);
    }

    StoreResourceDefinition(PathElement path, ResourceDescriptionResolver resolver, boolean allowRuntimeOnlyRegistration) {
        super(path, resolver);
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new StoreMetricExecutor(), StoreMetric.class).register(registration);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        new StoreWriteBehindResourceDefinition().register(registration);
        new StoreWriteThroughResourceDefinition().register(registration);

        new StorePropertyResourceDefinition().register(registration);
    }
}
