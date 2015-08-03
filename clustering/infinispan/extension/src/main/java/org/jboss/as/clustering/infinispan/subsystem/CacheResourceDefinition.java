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

import org.jboss.as.clustering.controller.AttributeMarshallers;
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
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
public class CacheResourceDefinition extends SimpleResourceDefinition implements Registration {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        @Deprecated BATCHING("batching", ModelType.BOOLEAN, new ModelNode(false), null, InfinispanModel.VERSION_3_0_0),
        MODULE("module", ModelType.STRING, null, new ModuleIdentifierValidator(true, true)),
        @Deprecated INDEXING("indexing", ModelType.STRING, IndexingResourceDefinition.Attribute.INDEX.getDefinition().getDefaultValue(), IndexingResourceDefinition.Attribute.INDEX.getDefinition().getValidator(), InfinispanModel.VERSION_4_0_0),
        @Deprecated INDEXING_PROPERTIES("indexing-properties", InfinispanModel.VERSION_4_0_0),
        JNDI_NAME("jndi-name", ModelType.STRING, null, null),
        @Deprecated START("start", ModelType.STRING, new ModelNode(StartMode.LAZY.name()), new EnumValidator<>(StartMode.class, true, false), InfinispanModel.VERSION_3_0_0),
        STATISTICS_ENABLED("statistics-enabled", ModelType.BOOLEAN, new ModelNode(false), null),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = createBuilder(name, type, defaultValue, validator).build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator, InfinispanModel deprecation) {
            this.definition = createBuilder(name, type, defaultValue, validator).setDeprecated(deprecation.getVersion()).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(validator)
            ;
        }

        Attribute(String name, InfinispanModel deprecation) {
            this.definition = new SimpleMapAttributeDefinition.Builder(name, true)
                    .setAllowExpression(true)
                    .setAttributeMarshaller(AttributeMarshallers.PROPERTY_LIST)
                    .setAttributeParser(AttributeParsers.COLLECTION)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.discardChildResource(NoStoreResourceDefinition.PATH);
        } else {
            NoStoreResourceDefinition.buildTransformation(version, builder);
        }

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Set batching=true if transaction mode=BATCH
            ResourceTransformer batchingTransformer = new ResourceTransformer() {
                @Override
                public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                    PathAddress transactionAddress = address.append(TransactionResourceDefinition.PATH);
                    try {
                        ModelNode transaction = context.readResourceFromRoot(transactionAddress).getModel();
                        if (transaction.hasDefined(TransactionResourceDefinition.Attribute.MODE.getDefinition().getName())) {
                            ModelNode mode = transaction.get(TransactionResourceDefinition.Attribute.MODE.getDefinition().getName());
                            if ((mode.getType() == ModelType.STRING) && (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH)) {
                                resource.getModel().get(Attribute.BATCHING.getDefinition().getName()).set(true);
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
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attribute.STATISTICS_ENABLED.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), Attribute.STATISTICS_ENABLED.getDefinition());
        }

        LockingResourceDefinition.buildTransformation(version, builder);
        EvictionResourceDefinition.buildTransformation(version, builder);
        ExpirationResourceDefinition.buildTransformation(version, builder);
        IndexingResourceDefinition.buildTransformation(version, builder);
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

    public CacheResourceDefinition(PathElement path, PathManager pathManager, boolean allowRuntimeOnlyRegistration) {
        super(path, new InfinispanResourceDescriptionResolver(path, PathElement.pathElement("cache")));
        this.pathManager = pathManager;
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new CacheMetricExecutor(), CacheMetric.class).register(registration);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registration) {
        new EvictionResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new ExpirationResourceDefinition().register(registration);
        new IndexingResourceDefinition().register(registration);
        new LockingResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new TransactionResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);

        new NoStoreResourceDefinition().register(registration);
        new CustomStoreResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new FileStoreResourceDefinition(this.pathManager, this.allowRuntimeOnlyRegistration).register(registration);
        new BinaryKeyedJDBCStoreResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new MixedKeyedJDBCStoreResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new StringKeyedJDBCStoreResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new RemoteStoreResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerSubModel(this);
    }
}
