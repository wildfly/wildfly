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

import org.infinispan.transaction.LockingMode;
import org.jboss.as.clustering.controller.AttributeOperationTransformer;
import org.jboss.as.clustering.controller.ChainedOperationTransformer;
import org.jboss.as.clustering.controller.OperationFactory;
import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/transaction=TRANSACTION
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransactionResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);

    // attributes
    // cache mode required, txn mode not
    static final SimpleAttributeDefinition LOCKING = new SimpleAttributeDefinitionBuilder(ModelKeys.LOCKING, ModelType.STRING, true)
            .setXmlName(Attribute.LOCKING.getLocalName())
            .setAllowExpression(true)
            .setValidator(new EnumValidator<>(LockingMode.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(LockingMode.PESSIMISTIC.name()))
            .build();

    static final SimpleAttributeDefinition MODE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING, true)
            .setXmlName(Attribute.MODE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(TransactionMode.class, true, true))
            .setDefaultValue(new ModelNode().set(TransactionMode.NONE.name()))
            .build();

    static final SimpleAttributeDefinition STOP_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.STOP_TIMEOUT, ModelType.LONG, true)
            .setXmlName(Attribute.STOP_TIMEOUT.getLocalName())
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(30000))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { MODE, STOP_TIMEOUT, LOCKING };

    private final boolean allowRuntimeOnlyRegistration;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        List<OperationTransformer> addOperationTransformers = new LinkedList<>();
        List<OperationTransformer> removeOperationTransformers = new LinkedList<>();
        Map<String, OperationTransformer> readAttributeTransformers = new HashMap<>();
        Map<String, OperationTransformer> writeAttributeTransformers = new HashMap<>();
        Map<String, OperationTransformer> undefineAttributeTransformers = new HashMap<>();

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Convert BATCH -> NONE, and include write-attribute:name=batching
            OperationTransformer addTransformer = new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                    if (operation.hasDefined(MODE.getName())) {
                        ModelNode mode = operation.get(MODE.getName());
                        boolean batching = (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH);
                        if (batching) {
                            mode.set(TransactionMode.NONE.name());
                            ModelNode writeBatchingOperation = OperationFactory.createWriteAttributeOperation(cacheAddress(address), CacheResourceDefinition.BATCHING.getName(), new ModelNode(true));
                            return new TransformedOperation(OperationFactory.createCompositeOperation(writeBatchingOperation, operation), OperationResultTransformer.ORIGINAL_RESULT);
                        }
                    }
                    return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
                }
            };
            addOperationTransformers.add(addTransformer);

            // Additionally include undefine-attribute:name=batching
            OperationTransformer removeTransformer = new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                    ModelNode undefineBatchingOperation = OperationFactory.createUndefineAttributeOperation(cacheAddress(address), CacheResourceDefinition.BATCHING.getName());
                    return new TransformedOperation(OperationFactory.createCompositeOperation(undefineBatchingOperation, operation), OperationResultTransformer.ORIGINAL_RESULT);
                }
            };
            removeOperationTransformers.add(removeTransformer);

            // If read-attribute:name=batching is true, return BATCH, otherwise use result from read-attribute:name=mode
            OperationTransformer readAttributeTransformer = new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                    ModelNode readBatchingOperation = OperationFactory.createReadAttributeOperation(cacheAddress(address), CacheResourceDefinition.BATCHING.getName());
                    OperationResultTransformer resultTransformer = new OperationResultTransformer() {
                        @Override
                        public ModelNode transformResult(ModelNode result) {
                            ModelNode readBatchingResult = result.get(0);
                            return readBatchingResult.asBoolean() ? new ModelNode(TransactionMode.BATCH.name()) : result.get(1);
                        }
                    };
                    return new TransformedOperation(OperationFactory.createCompositeOperation(readBatchingOperation, operation), resultTransformer);
                }
            };
            readAttributeTransformers.put(MODE.getName(), readAttributeTransformer);

            // Convert BATCH -> NONE, and include write-attribute:name=batching
            OperationTransformer writeAttributeTransformer = new OperationTransformer() {
                @Override
                public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                    ModelNode mode = operation.hasDefined(ModelDescriptionConstants.VALUE) ? operation.get(ModelDescriptionConstants.VALUE) : null;
                    boolean batching = (mode != null) ? (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH) : false;
                    if (batching) {
                        mode.set(TransactionMode.NONE.name());
                    }
                    ModelNode writeBatchingOperation = OperationFactory.createWriteAttributeOperation(cacheAddress(address), CacheResourceDefinition.BATCHING.getName(), new ModelNode(batching));
                    return new TransformedOperation(OperationFactory.createCompositeOperation(writeBatchingOperation, operation), OperationResultTransformer.ORIGINAL_RESULT);
                }
            };
            writeAttributeTransformers.put(MODE.getName(), writeAttributeTransformer);
            undefineAttributeTransformers.put(MODE.getName(), writeAttributeTransformer);

            // Convert BATCH -> NONE
            AttributeConverter modeConverter = new AttributeConverter() {
                @Override
                public void convertOperationParameter(PathAddress address, String name, ModelNode value, ModelNode operation, TransformationContext context) {
                    // Operation transformation is handled via custom operation transformers
                }

                @Override
                public void convertResourceAttribute(PathAddress address, String name, ModelNode value, TransformationContext context) {
                    if (value.isDefined()) {
                        TransactionMode mode = TransactionMode.valueOf(value.asString());
                        if (mode == TransactionMode.BATCH) {
                            value.set(TransactionMode.NONE.name());
                        }
                    }
                }
            };
            builder.getAttributeBuilder().setValueConverter(modeConverter, MODE);
        }
        if (InfinispanModel.VERSION_1_4_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, MODE, STOP_TIMEOUT, LOCKING);
        }

        buildOperationTransformation(builder, ModelDescriptionConstants.ADD, addOperationTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.REMOVE, removeOperationTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, readAttributeTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, writeAttributeTransformers);
        buildOperationTransformation(builder, ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, undefineAttributeTransformers);
    }

    static void buildOperationTransformation(ResourceTransformationDescriptionBuilder builder, String operationName, List<OperationTransformer> transformers) {
        if (!transformers.isEmpty()) {
            builder.addOperationTransformationOverride(operationName).setCustomOperationTransformer(new ChainedOperationTransformer(transformers)).inheritResourceAttributeDefinitions();
        }
    }

    static void buildOperationTransformation(ResourceTransformationDescriptionBuilder builder, String operationName, Map<String, OperationTransformer> transformers) {
        if (!transformers.isEmpty()) {
            builder.addOperationTransformationOverride(operationName).setCustomOperationTransformer(new AttributeOperationTransformer(transformers)).inheritResourceAttributeDefinitions();
        }
    }

    static PathAddress cacheAddress(PathAddress transactionAddress) {
        return transactionAddress.subAddress(0, transactionAddress.size() - 1);
    }

    TransactionResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.TRANSACTION), new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
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
            // register any metrics
            OperationStepHandler handler = new TransactionMetricsHandler();
            for (TransactionMetric metric: TransactionMetric.values()) {
                registration.registerMetric(metric.getDefinition(), handler);
            }
        }
    }
}
