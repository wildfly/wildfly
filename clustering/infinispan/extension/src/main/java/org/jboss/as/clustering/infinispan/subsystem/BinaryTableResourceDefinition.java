/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition of a binary table resource.
 * @author Paul Ferraro
 */
public class BinaryTableResourceDefinition extends TableResourceDefinition {

    static final PathElement PATH = pathElement("binary");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        PREFIX("prefix", ModelType.STRING, new ModelNode("ispn_bucket")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            OperationTransformer addTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress storeAddress = Operations.getPathAddress(operation).getParent();
                    ModelNode value = new ModelNode();
                    for (Class<? extends org.jboss.as.clustering.controller.Attribute> attributeClass : Arrays.asList(Attribute.class, TableResourceDefinition.Attribute.class, TableResourceDefinition.ColumnAttribute.class)) {
                        for (org.jboss.as.clustering.controller.Attribute attribute : attributeClass.getEnumConstants()) {
                            String name = attribute.getName();
                            if (operation.hasDefined(name)) {
                                value.get(name).set(operation.get(name));
                            }
                        }
                    }
                    return value.isDefined() ? Operations.createWriteAttributeOperation(storeAddress, BinaryKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE, value) : Operations.createUndefineAttributeOperation(storeAddress, BinaryKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE);
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.ADD, new SimpleOperationTransformer(addTransformer));

            OperationTransformer removeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress storeAddress = Operations.getPathAddress(operation).getParent();
                    return Operations.createUndefineAttributeOperation(storeAddress, BinaryKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE);
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.REMOVE, new SimpleOperationTransformer(removeTransformer));
        }
    }

    BinaryTableResourceDefinition() {
        super(PATH, new InfinispanResourceDescriptionResolver(PATH, WILDCARD_PATH));
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(TableResourceDefinition.Attribute.class)
                .addAttributes(TableResourceDefinition.ColumnAttribute.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new BinaryTableBuilder(address.getParent().getParent()));
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);
    }
}
