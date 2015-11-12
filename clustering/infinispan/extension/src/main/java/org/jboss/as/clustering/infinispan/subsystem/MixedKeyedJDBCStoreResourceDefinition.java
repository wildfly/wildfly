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

import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;
import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.LegacyPropertyAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.LegacyPropertyResourceTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=mixed-jdbc
 * /subsystem=infinispan/cache-container=X/cache=Y/mixed-keyed-jdbc-store=MIXED_KEYED_JDBC_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class MixedKeyedJDBCStoreResourceDefinition extends JDBCStoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("mixed-keyed-jdbc-store", "MIXED_KEYED_JDBC_STORE");
    static final PathElement PATH = pathElement("mixed-jdbc");

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        BINARY_TABLE(BinaryKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE),
        STRING_TABLE(StringKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(org.jboss.as.clustering.controller.Attribute attribute) {
            this.definition = attribute.getDefinition();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.setCustomResourceTransformer(new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    final ModelNode model = resource.getModel();

                    final ModelNode binaryTableModel = Resource.Tools.readModel(resource.removeChild(BinaryTableResourceDefinition.PATH));
                    if (binaryTableModel != null && binaryTableModel.isDefined()) {
                        model.get(DeprecatedAttribute.BINARY_TABLE.getDefinition().getName()).set(binaryTableModel);
                    }

                    final ModelNode stringTableModel = Resource.Tools.readModel(resource.removeChild(StringTableResourceDefinition.PATH));
                    if (stringTableModel != null && stringTableModel.isDefined()) {
                        model.get(DeprecatedAttribute.STRING_TABLE.getDefinition().getName()).set(stringTableModel);
                    }

                    final ModelNode properties = model.remove(StoreResourceDefinition.Attribute.PROPERTIES.getDefinition().getName());
                    final ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);

                    LegacyPropertyResourceTransformer.transformPropertiesToChildrenResources(properties, address, childContext);

                    context.processChildren(resource);
                }
            });
        }

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                    .setCustomOperationTransformer(new SimpleOperationTransformer(new LegacyPropertyAddOperationTransformer())).inheritResourceAttributeDefinitions();
        }

        BinaryTableResourceDefinition.buildTransformation(version, builder);
        StringTableResourceDefinition.buildTransformation(version, builder);

        JDBCStoreResourceDefinition.buildTransformation(version, builder);
    }

    MixedKeyedJDBCStoreResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(PATH, pathElement("jdbc"), WILDCARD_PATH), allowRuntimeOnlyRegistration);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        parentRegistration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(JDBCStoreResourceDefinition.Attribute.class)
                .addAttributes(StoreResourceDefinition.Attribute.class)
                .addExtraParameters(DeprecatedAttribute.class)
                .addExtraParameters(JDBCStoreResourceDefinition.DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(new MixedKeyedJDBCStoreBuilderFactory());
        new AddStepHandler(descriptor, handler) {
            @Override
            protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                translateAddOperation(context, operation);
                super.populateModel(context, operation, resource);
            }

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                super.execute(context, operation);
                // Translate deprecated BINARY_TABLE attribute into separate add table operation
                this.addTableStep(context, operation, DeprecatedAttribute.BINARY_TABLE, BinaryTableResourceDefinition.PATH, new BinaryTableBuilderFactory());
                // Translate deprecated STRING_TABLE attribute into separate add table operation
                this.addTableStep(context, operation, DeprecatedAttribute.STRING_TABLE, StringTableResourceDefinition.PATH, new StringTableBuilderFactory());
            }

            private void addTableStep(OperationContext context, ModelNode operation, DeprecatedAttribute attribute, PathElement path, ResourceServiceBuilderFactory<TableManipulationConfiguration> provider) {
                if (operation.hasDefined(attribute.getDefinition().getName())) {
                    ModelNode addTableOperation = Util.createAddOperation(context.getCurrentAddress().append(path));
                    ModelNode parameters = operation.get(attribute.getDefinition().getName());
                    for (Property parameter : parameters.asPropertyList()) {
                        addTableOperation.get(parameter.getName()).set(parameter.getValue());
                    }
                    context.addStep(addTableOperation, registration.getOperationHandler(PathAddress.pathAddress(path), ModelDescriptionConstants.ADD), context.getCurrentStage());
                }
            }
        }.register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);

        registration.registerReadWriteAttribute(DeprecatedAttribute.BINARY_TABLE.getDefinition(), BinaryKeyedJDBCStoreResourceDefinition.LEGACY_READ_TABLE_HANDLER, BinaryKeyedJDBCStoreResourceDefinition.LEGACY_WRITE_TABLE_HANDLER);
        registration.registerReadWriteAttribute(DeprecatedAttribute.STRING_TABLE.getDefinition(), StringKeyedJDBCStoreResourceDefinition.LEGACY_READ_TABLE_HANDLER, StringKeyedJDBCStoreResourceDefinition.LEGACY_WRITE_TABLE_HANDLER);

        new BinaryTableResourceDefinition().register(registration);
        new StringTableResourceDefinition().register(registration);

        super.register(registration);
    }
}
