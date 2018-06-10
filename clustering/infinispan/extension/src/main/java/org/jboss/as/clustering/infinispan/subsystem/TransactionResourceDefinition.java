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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.infinispan.transaction.LockingMode;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.AttributeOperationTransformer;
import org.jboss.as.clustering.controller.transform.ChainedOperationTransformer;
import org.jboss.as.clustering.controller.transform.OperationTransformer;
import org.jboss.as.clustering.controller.transform.RequiredChildResourceDiscardPolicy;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/component=transaction
 * /subsystem=infinispan/cache-container=X/cache=Y/transaction=TRANSACTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransactionResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("transaction");
    static final PathElement LEGACY_PATH = PathElement.pathElement(PATH.getValue(), "TRANSACTION");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        LOCKING("locking", ModelType.STRING, new ModelNode(LockingMode.PESSIMISTIC.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(LockingMode.class));
            }
        },
        MODE("mode", ModelType.STRING, new ModelNode(TransactionMode.NONE.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new EnumValidator<>(TransactionMode.class));
            }
        },
        STOP_TIMEOUT("stop-timeout", ModelType.LONG, new ModelNode(10000L)) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
        ;
        private final SimpleAttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH, RequiredChildResourceDiscardPolicy.NEVER) : parent.addChildResource(PATH);

        List<org.jboss.as.controller.transform.OperationTransformer> addOperationTransformers = new LinkedList<>();
        List<org.jboss.as.controller.transform.OperationTransformer> removeOperationTransformers = new LinkedList<>();
        Map<String, org.jboss.as.controller.transform.OperationTransformer> readAttributeTransformers = new HashMap<>();
        Map<String, org.jboss.as.controller.transform.OperationTransformer> writeAttributeTransformers = new HashMap<>();
        Map<String, org.jboss.as.controller.transform.OperationTransformer> undefineAttributeTransformers = new HashMap<>();

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Convert BATCH -> NONE, and include write-attribute:name=batching
            OperationTransformer addTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    if (operation.hasDefined(Attribute.MODE.getName())) {
                        ModelNode mode = operation.get(Attribute.MODE.getName());
                        if ((mode.getType() == ModelType.STRING) && (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH)) {
                            mode.set(TransactionMode.NONE.name());
                            PathAddress address = Operations.getPathAddress(operation);
                            return Operations.createCompositeOperation(operation, Operations.createWriteAttributeOperation(cacheAddress(address), CacheResourceDefinition.DeprecatedAttribute.BATCHING, new ModelNode(true)));
                        }
                    }
                    return operation;
                }
            };
            addOperationTransformers.add(new SimpleOperationTransformer(addTransformer));

            // Additionally include undefine-attribute:name=batching
            OperationTransformer removeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    return Operations.createCompositeOperation(operation, Operations.createUndefineAttributeOperation(cacheAddress(address), CacheResourceDefinition.DeprecatedAttribute.BATCHING));
                }
            };
            removeOperationTransformers.add(new SimpleOperationTransformer(removeTransformer));

            // If read-attribute:name=batching is true, return BATCH, otherwise use result from read-attribute:name=mode
            OperationTransformer readAttributeOperationTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    return Operations.createCompositeOperation(Operations.createReadAttributeOperation(cacheAddress(address), CacheResourceDefinition.DeprecatedAttribute.BATCHING), operation);
                }
            };
            OperationResultTransformer readAttributeResultTransformer = new OperationResultTransformer() {
                @Override
                public ModelNode transformResult(ModelNode result) {
                    ModelNode readBatchingResult = result.get(0);
                    return readBatchingResult.asBoolean() ? new ModelNode(TransactionMode.BATCH.name()) : result.get(1);
                }
            };
            readAttributeTransformers.put(Attribute.MODE.getName(), new SimpleOperationTransformer(readAttributeOperationTransformer, readAttributeResultTransformer));

            // Convert BATCH -> NONE, and include write-attribute:name=batching
            OperationTransformer writeAttributeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    ModelNode mode = Operations.getAttributeValue(operation);
                    boolean batching = (mode.isDefined() && (mode.getType() == ModelType.STRING)) ? (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH) : false;
                    if (batching) {
                        mode.set(TransactionMode.NONE.name());
                    }
                    PathAddress address = Operations.getPathAddress(operation);
                    return Operations.createCompositeOperation(operation, Operations.createWriteAttributeOperation(cacheAddress(address), CacheResourceDefinition.DeprecatedAttribute.BATCHING, new ModelNode(batching)));
                }
            };
            writeAttributeTransformers.put(Attribute.MODE.getName(), new SimpleOperationTransformer(writeAttributeTransformer));

            // Include undefine-attribute:name=batching
            OperationTransformer undefineAttributeTransformer = new OperationTransformer() {
                @Override
                public ModelNode transformOperation(ModelNode operation) {
                    PathAddress address = Operations.getPathAddress(operation);
                    return Operations.createCompositeOperation(operation, Operations.createUndefineAttributeOperation(cacheAddress(address), CacheResourceDefinition.DeprecatedAttribute.BATCHING));
                }
            };
            undefineAttributeTransformers.put(Attribute.MODE.getName(), new SimpleOperationTransformer(undefineAttributeTransformer));

            // Convert BATCH -> NONE
            ResourceTransformer modeTransformer = new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    ModelNode model = resource.getModel();
                    if (model.hasDefined(Attribute.MODE.getName())) {
                        ModelNode value = model.get(Attribute.MODE.getName());
                        if ((value.getType() == ModelType.STRING) && (TransactionMode.valueOf(value.asString()) == TransactionMode.BATCH)) {
                            value.set(TransactionMode.NONE.name());
                        }
                    }
                    context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource).processChildren(resource);
                }
            };
            builder.setCustomResourceTransformer(modeTransformer);

            // change default value of stop-timeout attribute
            builder.getAttributeBuilder().setValueConverter(new DefaultValueAttributeConverter(Attribute.STOP_TIMEOUT.getDefinition()), Attribute.STOP_TIMEOUT.getDefinition());
        }

        buildOperationTransformation(builder, ModelDescriptionConstants.ADD, addOperationTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.REMOVE, removeOperationTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, readAttributeTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, writeAttributeTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, undefineAttributeTransformers);
    }

    static void buildOperationTransformation(ResourceTransformationDescriptionBuilder builder, String operationName, List<org.jboss.as.controller.transform.OperationTransformer> transformers) {
        if (!transformers.isEmpty()) {
            builder.addOperationTransformationOverride(operationName).setCustomOperationTransformer(new ChainedOperationTransformer(transformers)).inheritResourceAttributeDefinitions();
        }
    }

    static void buildOperationTransformation(ResourceTransformationDescriptionBuilder builder, String operationName, Map<String, org.jboss.as.controller.transform.OperationTransformer> transformers) {
        if (!transformers.isEmpty()) {
            builder.addOperationTransformationOverride(operationName).setCustomOperationTransformer(new AttributeOperationTransformer(transformers)).inheritResourceAttributeDefinitions();
        }
    }

    static PathAddress cacheAddress(PathAddress transactionAddress) {
        return transactionAddress.subAddress(0, transactionAddress.size() - 1);
    }

    TransactionResourceDefinition() {
        super(PATH);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        parent.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver()).addAttributes(Attribute.class);
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(TransactionServiceConfigurator::new);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new TransactionMetricExecutor(), TransactionMetric.class).register(registration);
        }

        return registration;
    }
}
