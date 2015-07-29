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
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Resource description for the addressable resource
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=mixed
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class MixedKeyedJDBCStoreResourceDefinition extends JDBCStoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("mixed-keyed-jdbc-store", "MIXED_KEYED_JDBC_STORE");
    static final PathElement PATH = pathElement("mixed-jdbc");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        @Deprecated BINARY_TABLE(BinaryKeyedJDBCStoreResourceDefinition.Attribute.TABLE),
        @Deprecated STRING_TABLE(StringKeyedJDBCStoreResourceDefinition.Attribute.TABLE),
        ;
        private final AttributeDefinition definition;

        Attribute(org.jboss.as.clustering.controller.Attribute attribute) {
            this.definition = attribute.getDefinition();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

        JDBCStoreResourceDefinition.buildTransformation(version, builder);

        BinaryTableResourceDefinition.buildTransformation(version, builder);
        StringTableResourceDefinition.buildTransformation(version, builder);
    }

    MixedKeyedJDBCStoreResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(PATH, pathElement("jdbc"), WILDCARD_PATH), allowRuntimeOnlyRegistration);
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(JDBCStoreResourceDefinition.Attribute.class).addAttributes(StoreResourceDefinition.Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(new MixedKeyedJDBCStoreBuilderFactory());
        new AddStepHandler(descriptor, handler) {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                super.execute(context, operation);
                // Translate deprecated BINARY_TABLE attribute into separate add table operation
                this.addTableStep(context, operation, Attribute.BINARY_TABLE, BinaryTableResourceDefinition.PATH, new BinaryTableBuilderFactory());
                // Translate deprecated STRING_TABLE attribute into separate add table operation
                this.addTableStep(context, operation, Attribute.STRING_TABLE, StringTableResourceDefinition.PATH, new StringTableBuilderFactory());
            }

            private void addTableStep(OperationContext context, ModelNode operation, Attribute attribute, PathElement path, ResourceServiceBuilderFactory<TableManipulationConfiguration> provider) {
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
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        super.registerChildren(registration);
        new BinaryTableResourceDefinition().register(registration);
        new StringTableResourceDefinition().register(registration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        registration.registerReadWriteAttribute(Attribute.BINARY_TABLE.getDefinition(), BinaryKeyedJDBCStoreResourceDefinition.LEGACY_READ_TABLE_HANDLER, BinaryKeyedJDBCStoreResourceDefinition.LEGACY_WRITE_TABLE_HANDLER);
        registration.registerReadWriteAttribute(Attribute.STRING_TABLE.getDefinition(), StringKeyedJDBCStoreResourceDefinition.LEGACY_READ_TABLE_HANDLER, StringKeyedJDBCStoreResourceDefinition.LEGACY_WRITE_TABLE_HANDLER);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration.registerSubModel(this)));
    }
}
