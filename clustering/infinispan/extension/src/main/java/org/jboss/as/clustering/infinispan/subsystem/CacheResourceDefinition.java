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

import java.util.NoSuchElementException;

import org.infinispan.configuration.cache.Index;
import org.jboss.as.clustering.controller.AttributeMarshallers;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResourceDefinition extends SimpleResourceDefinition {

    @Deprecated
    static final SimpleAttributeDefinition BATCHING = new SimpleAttributeDefinitionBuilder(ModelKeys.BATCHING, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.BATCHING.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
            .setXmlName(Attribute.MODULE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new ModuleIdentifierValidator(true))
            .build();

    static final SimpleAttributeDefinition INDEXING = new SimpleAttributeDefinitionBuilder(ModelKeys.INDEXING, ModelType.STRING, true)
            .setXmlName(Attribute.INDEX.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(Index.class, true, false))
            .setDefaultValue(new ModelNode().set(Index.NONE.name()))
            .build();

    static final SimpleMapAttributeDefinition INDEXING_PROPERTIES = new SimpleMapAttributeDefinition.Builder(ModelKeys.INDEXING_PROPERTIES, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshallers.PROPERTY_LIST)
            .build();

    static final SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING, true)
            .setXmlName(Attribute.JNDI_NAME.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    @Deprecated
    static final SimpleAttributeDefinition START = new SimpleAttributeDefinitionBuilder(ModelKeys.START, ModelType.STRING, true)
            .setXmlName(Attribute.START.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(StartMode.class, true, false))
            .setDefaultValue(new ModelNode().set(StartMode.LAZY.name()))
            .setDeprecated(InfinispanModel.VERSION_3_0_0.getVersion())
            .build();

    static final SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(ModelKeys.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.STATISTICS_ENABLED.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode().set(false))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] {
            BATCHING, MODULE, INDEXING, INDEXING_PROPERTIES, JNDI_NAME, START, STATISTICS_ENABLED
    };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Set batching=true if transaction mode=BATCH
            ResourceTransformer batchingTransformer = new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    PathAddress transactionAddress = address.append(TransactionResourceDefinition.PATH);
                    try {
                        ModelNode transaction = context.readResourceFromRoot(transactionAddress).getModel();
                        if (transaction.hasDefined(TransactionResourceDefinition.MODE.getName())) {
                            ModelNode mode = transaction.get(TransactionResourceDefinition.MODE.getName());
                            if ((mode.getType() == ModelType.STRING) && (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH)) {
                                resource.getModel().get(BATCHING.getName()).set(true);
                            }
                        }
                    } catch (NoSuchElementException e) {
                        // Ignore, nothing to convert
                    }
                    context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource).processChildren(resource);
                }
            };
            builder.setCustomResourceTransformer(batchingTransformer);
        }

        if (InfinispanModel.VERSION_1_5_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, STATISTICS_ENABLED)
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, STATISTICS_ENABLED)
                    .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), STATISTICS_ENABLED);
        }

        LockingResourceDefinition.buildTransformation(version, builder);
        EvictionResourceDefinition.buildTransformation(version, builder);
        ExpirationResourceDefinition.buildTransformation(version, builder);
        TransactionResourceDefinition.buildTransformation(version, builder);

        FileStoreResourceDefinition.buildTransformation(version, builder);
        BinaryKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        MixedKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        StringKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        RemoteStoreResourceDefinition.buildTransformation(version, builder);
        CustomStoreResourceDefinition.buildTransformation(version, builder);
    }

    private final PathManager pathManager;
    final boolean allowRuntimeOnlyRegistration;

    public CacheResourceDefinition(CacheType type, PathManager pathManager, boolean allowRuntimeOnlyRegistration) {
        super(type.pathElement(), type.getResourceDescriptionResolver(), type.getAddHandler(), type.getRemoveHandler());
        this.pathManager = pathManager;
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new CacheMetricExecutor(), CacheMetric.class).register(registration);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        registration.registerSubModel(new LockingResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new TransactionResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new EvictionResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new ExpirationResourceDefinition());

        registration.registerSubModel(new CustomStoreResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new FileStoreResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new StringKeyedJDBCStoreResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new BinaryKeyedJDBCStoreResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new MixedKeyedJDBCStoreResourceDefinition(this.allowRuntimeOnlyRegistration));
        registration.registerSubModel(new RemoteStoreResourceDefinition(this.allowRuntimeOnlyRegistration));
    }
}
