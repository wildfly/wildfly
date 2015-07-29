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

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Definition of a binary JDBC cache store resource.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class BinaryKeyedJDBCStoreResourceDefinition extends JDBCStoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("binary-keyed-jdbc-store", "BINARY_KEYED_JDBC_STORE");
    static final PathElement PATH = pathElement("binary-jdbc");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        @Deprecated TABLE("binary-keyed-table", BinaryTableResourceDefinition.Attribute.values(), TableResourceDefinition.Attribute.values(), TableResourceDefinition.ColumnAttribute.values()),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, org.jboss.as.clustering.controller.Attribute[]... attributeSets) {
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
                    .setAllowNull(true)
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
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildResource(PATH) : parent.addChildRedirection(PATH, LEGACY_PATH);

        JDBCStoreResourceDefinition.buildTransformation(version, builder);

        BinaryTableResourceDefinition.buildTransformation(version, builder);
    }

    BinaryKeyedJDBCStoreResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(PATH, pathElement("jdbc"), WILDCARD_PATH), allowRuntimeOnlyRegistration);
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(JDBCStoreResourceDefinition.Attribute.class).addAttributes(StoreResourceDefinition.Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(new BinaryKeyedJDBCStoreBuilderFactory());
        new AddStepHandler(descriptor, handler) {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                super.execute(context, operation);
                if (operation.hasDefined(Attribute.TABLE.getDefinition().getName())) {
                    // Translate deprecated TABLE attribute into separate add table operation
                    ModelNode addTableOperation = Util.createAddOperation(context.getCurrentAddress().append(BinaryTableResourceDefinition.PATH));
                    ModelNode parameters = operation.get(Attribute.TABLE.getDefinition().getName());
                    for (Property parameter : parameters.asPropertyList()) {
                        addTableOperation.get(parameter.getName()).set(parameter.getValue());
                    }
                    context.addStep(addTableOperation, registration.getOperationHandler(PathAddress.pathAddress(BinaryTableResourceDefinition.PATH), ModelDescriptionConstants.ADD), context.getCurrentStage());
                }
            }
        }.register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);
    }

    static final OperationStepHandler LEGACY_READ_TABLE_HANDLER = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress().append(BinaryTableResourceDefinition.PATH);
            ModelNode readResourceOperation = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);
            operation.get(ModelDescriptionConstants.ATTRIBUTES_ONLY).set(true);
            context.addStep(readResourceOperation, new ReadResourceHandler(), context.getCurrentStage());
        }
    };

    static final OperationStepHandler LEGACY_WRITE_TABLE_HANDLER = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress address = context.getCurrentAddress().append(BinaryTableResourceDefinition.PATH);
            ModelNode table = Operations.getAttributeValue(operation);
            for (Class<? extends org.jboss.as.clustering.controller.Attribute> attributeClass : Arrays.asList(BinaryTableResourceDefinition.Attribute.class, TableResourceDefinition.Attribute.class)) {
                for (org.jboss.as.clustering.controller.Attribute attribute : attributeClass.getEnumConstants()) {
                    ModelNode writeAttributeOperation = Operations.createWriteAttributeOperation(address, attribute, table.get(attribute.getDefinition().getName()));
                    context.addStep(writeAttributeOperation, new ReloadRequiredWriteAttributeHandler(attribute), context.getCurrentStage());
                }
            }
        }
    };

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        super.registerChildren(registration);
        new BinaryTableResourceDefinition().register(registration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        registration.registerReadWriteAttribute(Attribute.TABLE.getDefinition(), LEGACY_READ_TABLE_HANDLER, LEGACY_WRITE_TABLE_HANDLER);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration.registerSubModel(this)));
    }
}
