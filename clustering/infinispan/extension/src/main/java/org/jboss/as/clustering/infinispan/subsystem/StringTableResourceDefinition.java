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

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class StringTableResourceDefinition extends TableResourceDefinition {

    static final PathElement PATH = pathElement("string");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        PREFIX("prefix", ModelType.STRING, new ModelNode("ispn_entry")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
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

        TableResourceDefinition.buildTransformation(version, builder);

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
                    return value.isDefined() ? Operations.createWriteAttributeOperation(storeAddress, StringKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE, value) : Operations.createUndefineAttributeOperation(storeAddress, StringKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE);
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.ADD, new SimpleOperationTransformer(addTransformer));

            OperationTransformer removeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress storeAddress = Operations.getPathAddress(operation).getParent();
                    return Operations.createUndefineAttributeOperation(storeAddress, StringKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.TABLE);
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.REMOVE, new SimpleOperationTransformer(removeTransformer));
        }
    }

    StringTableResourceDefinition() {
        super(PATH, Attribute.PREFIX);
    }
}
