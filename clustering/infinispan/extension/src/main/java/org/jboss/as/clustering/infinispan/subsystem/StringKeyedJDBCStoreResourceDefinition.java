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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.transform.LegacyPropertyResourceTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Resource description for the addressable resource and its legacy alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=string-jdbc
 * /subsystem=infinispan/cache-container=X/cache=Y/string-keyed-jdbc-store=STRING_KEYED_JDBC_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StringKeyedJDBCStoreResourceDefinition extends JDBCStoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("string-keyed-jdbc-store", "STRING_KEYED_JDBC_STORE");
    static final PathElement STRING_JDBC_PATH = pathElement("string-jdbc");
    static final PathElement PATH = JDBCStoreResourceDefinition.PATH;

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        TABLE("string-keyed-table", StringTableResourceDefinition.Attribute.values(), TableResourceDefinition.Attribute.values(), TableResourceDefinition.DeprecatedAttribute.values(), TableResourceDefinition.ColumnAttribute.values()),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, org.jboss.as.clustering.controller.Attribute[]... attributeSets) {
            int size = 0;
            for (org.jboss.as.clustering.controller.Attribute[] attributes : attributeSets) {
                size += attributes.length;
            }
            List<AttributeDefinition> definitions = new ArrayList<>(size);
            for (org.jboss.as.clustering.controller.Attribute[] attributes : attributeSets) {
                for (org.jboss.as.clustering.controller.Attribute attribute : attributes) {
                    definitions.add(attribute.getDefinition());
                }
            }
            this.definition = ObjectTypeAttributeDefinition.Builder.of(name, definitions.toArray(new AttributeDefinition[size]))
                    .setRequired(false)
                    .setDeprecated(InfinispanModel.VERSION_4_0_0.getVersion())
                    .setSuffix("table")
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : (InfinispanModel.VERSION_5_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, STRING_JDBC_PATH) : parent.addChildResource(PATH));

        JDBCStoreResourceDefinition.buildTransformation(version, builder, PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.setCustomResourceTransformer(new ResourceTransformer() {
                @SuppressWarnings("deprecation")
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    final ModelNode model = resource.getModel();
                    final ModelNode maxBatchSize = model.remove(StoreResourceDefinition.Attribute.MAX_BATCH_SIZE.getName());

                    final ModelNode stringTableModel = Resource.Tools.readModel(resource.removeChild(StringTableResourceDefinition.PATH));
                    if (stringTableModel != null && stringTableModel.isDefined()) {
                        model.get(DeprecatedAttribute.TABLE.getName()).set(stringTableModel);
                        model.get(DeprecatedAttribute.TABLE.getName()).get(TableResourceDefinition.DeprecatedAttribute.BATCH_SIZE.getName()).set((maxBatchSize != null) ? maxBatchSize : new ModelNode());
                    }

                    final ModelNode properties = model.remove(StoreResourceDefinition.Attribute.PROPERTIES.getName());
                    final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);

                    LegacyPropertyResourceTransformer.transformPropertiesToChildrenResources(properties, address, childContext);
                    context.processChildren(resource);
                }
            });
        }

        StringTableResourceDefinition.buildTransformation(version, builder);
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addExtraParameters(DeprecatedAttribute.class)
                    .addRequiredChildren(StringTableResourceDefinition.PATH)
                    // Translate deprecated TABLE attribute into separate add table operation
                    .setAddOperationTransformation(new TableAttributeTransformation(DeprecatedAttribute.TABLE, StringTableResourceDefinition.PATH))
                    ;
        }
    }

    StringKeyedJDBCStoreResourceDefinition() {
        super(PATH, LEGACY_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new ResourceDescriptorConfigurator());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        parent.registerAlias(STRING_JDBC_PATH, new SimpleAliasEntry(registration));

        registration.registerReadWriteAttribute(DeprecatedAttribute.TABLE.getDefinition(), LEGACY_READ_TABLE_HANDLER, LEGACY_WRITE_TABLE_HANDLER);

        new StringTableResourceDefinition().register(registration);

        return registration;
    }

    static final OperationStepHandler LEGACY_READ_TABLE_HANDLER = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress().append(StringTableResourceDefinition.PATH);
            ModelNode readResourceOperation = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);
            operation.get(ModelDescriptionConstants.ATTRIBUTES_ONLY).set(true);
            context.addStep(readResourceOperation, new ReadResourceHandler(), context.getCurrentStage());
        }
    };

    static final OperationStepHandler LEGACY_WRITE_TABLE_HANDLER = new OperationStepHandler() {
        @SuppressWarnings("deprecation")
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress().append(StringTableResourceDefinition.PATH);
            ModelNode table = Operations.getAttributeValue(operation);
            for (Class<? extends org.jboss.as.clustering.controller.Attribute> attributeClass : Arrays.asList(StringTableResourceDefinition.Attribute.class, TableResourceDefinition.Attribute.class, TableResourceDefinition.DeprecatedAttribute.class)) {
                for (org.jboss.as.clustering.controller.Attribute attribute : attributeClass.getEnumConstants()) {
                    ModelNode writeAttributeOperation = Operations.createWriteAttributeOperation(address, attribute, table.get(attribute.getName()));
                    context.addStep(writeAttributeOperation, context.getResourceRegistration().getAttributeAccess(PathAddress.pathAddress(StringTableResourceDefinition.PATH), attribute.getName()).getWriteHandler(), context.getCurrentStage());
                }
            }
        }
    };
}
