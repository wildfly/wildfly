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
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.BinaryRequirementCapability;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.validation.EnumValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
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
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.BinaryRequirement;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResourceDefinition extends ChildResourceDefinition {

    enum Capability implements CapabilityProvider {
        CACHE(InfinispanCacheRequirement.CACHE),
        CONFIGURATION(InfinispanCacheRequirement.CONFIGURATION),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(BinaryRequirement requirement) {
            this.capability = new BinaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        MODULE("module", ModelType.STRING, null, new ModuleIdentifierValidatorBuilder()),
        JNDI_NAME("jndi-name", ModelType.STRING, null),
        STATISTICS_ENABLED("statistics-enabled", ModelType.BOOLEAN, new ModelNode(false)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = createBuilder(name, type, defaultValue).build();
        }

        Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validator) {
            SimpleAttributeDefinitionBuilder builder = createBuilder(name, type, defaultValue);
            this.definition = builder.setValidator(validator.configure(builder).build()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute {
        @Deprecated BATCHING("batching", ModelType.BOOLEAN, new ModelNode(false), InfinispanModel.VERSION_3_0_0),
        @Deprecated INDEXING("indexing", ModelType.STRING, new ModelNode(Index.NONE.name()), new EnumValidatorBuilder<>(Index.class), InfinispanModel.VERSION_4_0_0),
        @Deprecated INDEXING_PROPERTIES("indexing-properties", InfinispanModel.VERSION_4_0_0),
        @Deprecated START("start", ModelType.STRING, new ModelNode(StartMode.LAZY.name()), new EnumValidatorBuilder<>(StartMode.class), InfinispanModel.VERSION_3_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanModel deprecation) {
            this(createBuilder(name, type, defaultValue), deprecation);
        }

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, ParameterValidatorBuilder validator, InfinispanModel deprecation) {
            this(createBuilder(name, type, defaultValue), validator, deprecation);
        }

        DeprecatedAttribute(String name, InfinispanModel deprecation) {
            this(new SimpleMapAttributeDefinition.Builder(name, true)
                    .setAllowNull(true)
                    .setAttributeMarshaller(AttributeMarshallers.PROPERTY_LIST)
                    .setAttributeParser(AttributeParsers.COLLECTION)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES), deprecation);
        }

        DeprecatedAttribute(AbstractAttributeDefinitionBuilder<?, ?> builder, ParameterValidatorBuilder validator, InfinispanModel deprecation) {
            this(builder.setValidator(validator.configure(builder).build()), deprecation);
        }

        DeprecatedAttribute(AbstractAttributeDefinitionBuilder<?, ?> builder, InfinispanModel deprecation) {
            this.definition = builder.setDeprecated(deprecation.getVersion()).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, ModelNode defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                ;
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
                        if (transaction.hasDefined(TransactionResourceDefinition.Attribute.MODE.getName())) {
                            ModelNode mode = transaction.get(TransactionResourceDefinition.Attribute.MODE.getName());
                            if ((mode.getType() == ModelType.STRING) && (TransactionMode.valueOf(mode.asString()) == TransactionMode.BATCH)) {
                                resource.getModel().get(DeprecatedAttribute.BATCHING.getName()).set(true);
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
    public void register(ManagementResourceRegistration registration) {

        if (this.allowRuntimeOnlyRegistration) {
            new MetricHandler<>(new CacheMetricExecutor(), CacheMetric.class).register(registration);
        }

        new EvictionResourceDefinition(this.allowRuntimeOnlyRegistration).register(registration);
        new ExpirationResourceDefinition().register(registration);
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
}
