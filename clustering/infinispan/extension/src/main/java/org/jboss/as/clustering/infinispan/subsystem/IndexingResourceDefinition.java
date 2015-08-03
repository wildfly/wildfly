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

import org.infinispan.configuration.cache.Index;
import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.AttributeMarshallers;
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class IndexingResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("indexing");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        INDEX("index", ModelType.STRING, new ModelNode(Index.NONE.name()), new EnumValidator<>(Index.class, true, false)),
        PROPERTIES("properties"),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(validator)
                    .build();
        }

        Attribute(String name) {
            this.definition = new SimpleMapAttributeDefinition.Builder(name, true)
                    .setAllowNull(true)
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

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            OperationTransformer addTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress cacheAddress = Operations.getPathAddress(operation).getParent();
                    ModelNode indexOperation = this.translateParameter(cacheAddress, operation, Attribute.INDEX, CacheResourceDefinition.Attribute.INDEXING);
                    ModelNode propertiesOperation = this.translateParameter(cacheAddress, operation, Attribute.PROPERTIES, CacheResourceDefinition.Attribute.INDEXING_PROPERTIES);
                    return Operations.createCompositeOperation(indexOperation, propertiesOperation);
                }

                private ModelNode translateParameter(PathAddress address, ModelNode operation, Attribute attribute, org.jboss.as.clustering.controller.Attribute legacyAttribute) {
                    String name = attribute.getDefinition().getName();
                    return operation.hasDefined(name) ? Operations.createWriteAttributeOperation(address, legacyAttribute, operation.get(name)) : Operations.createUndefineAttributeOperation(address, legacyAttribute);
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.ADD, new SimpleOperationTransformer(addTransformer));

            OperationTransformer removeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress cacheAddress = Operations.getPathAddress(operation).getParent();
                    ModelNode indexOperation = Operations.createUndefineAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING);
                    ModelNode propertiesOperation = Operations.createUndefineAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING_PROPERTIES);
                    return Operations.createCompositeOperation(indexOperation, propertiesOperation);
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.REMOVE, new SimpleOperationTransformer(removeTransformer));

            OperationTransformer readAttributeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress cacheAddress = Operations.getPathAddress(operation).getParent();
                    String name = Operations.getAttributeName(operation);
                    if (Attribute.INDEX.getDefinition().getName().equals(name)) {
                        return Operations.createReadAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING);
                    } else if (Attribute.PROPERTIES.getDefinition().getName().equals(name)) {
                        return Operations.createReadAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING_PROPERTIES);
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, new SimpleOperationTransformer(readAttributeTransformer));

            OperationTransformer writeAttributeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress cacheAddress = Operations.getPathAddress(operation).getParent();
                    String name = Operations.getAttributeName(operation);
                    ModelNode value = Operations.getAttributeValue(operation);
                    if (Attribute.INDEX.getDefinition().getName().equals(name)) {
                        return Operations.createWriteAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING, value);
                    } else if (Attribute.PROPERTIES.getDefinition().getName().equals(name)) {
                        return Operations.createWriteAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING_PROPERTIES, value);
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, new SimpleOperationTransformer(writeAttributeTransformer));

            OperationTransformer undefineAttributeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress cacheAddress = Operations.getPathAddress(operation).getParent();
                    String name = Operations.getAttributeName(operation);
                    if (Attribute.INDEX.getDefinition().getName().equals(name)) {
                        return Operations.createUndefineAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING);
                    } else if (Attribute.PROPERTIES.getDefinition().getName().equals(name)) {
                        return Operations.createUndefineAttributeOperation(cacheAddress, CacheResourceDefinition.Attribute.INDEXING_PROPERTIES);
                    }
                    return operation;
                }
            };
            builder.addRawOperationTransformationOverride(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, new SimpleOperationTransformer(undefineAttributeTransformer));
        }
    }

    IndexingResourceDefinition() {
        super(PATH);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(new IndexingBuilderFactory());
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerSubModel(this);
    }
}
