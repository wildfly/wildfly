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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.infinispan.lifecycle.ComponentStatus;
import org.jboss.as.clustering.controller.AttributeTranslation;
import org.jboss.as.clustering.controller.BinaryRequirementCapability;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.ListAttributeTranslation;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ReadAttributeTranslationHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

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

    static final Map<ClusteringCacheRequirement, org.jboss.as.clustering.controller.Capability> CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringCacheRequirement.class);
    static {
        for (ClusteringCacheRequirement requirement : EnumSet.allOf(ClusteringCacheRequirement.class)) {
            CLUSTERING_CAPABILITIES.put(requirement, new BinaryRequirementCapability(requirement));
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(ModelNode.FALSE);
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(createBuilder(name, type))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    enum ListAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<StringListAttributeDefinition.Builder> {
        MODULES("modules") {
            @Override
            public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
                return builder.setElementValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        ListAttribute(String name) {
            this.definition = this.apply(new StringListAttributeDefinition.Builder(name)
                    .setRequired(false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public StringListAttributeDefinition.Builder apply(StringListAttributeDefinition.Builder builder) {
            return builder;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        BATCHING("batching", ModelType.BOOLEAN, InfinispanModel.VERSION_3_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(ModelNode.FALSE);
            }
        },
        INDEXING("indexing", ModelType.STRING, InfinispanModel.VERSION_4_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(org.infinispan.configuration.cache.Index.NONE.name()))
                        .setValidator(new EnumValidator<>(org.infinispan.configuration.cache.Index.class))
                        ;
            }
        },
        INDEXING_PROPERTIES("indexing-properties", InfinispanModel.VERSION_4_0_0),
        JNDI_NAME("jndi-name", ModelType.STRING, InfinispanModel.VERSION_6_0_0),
        MODULE(ModelDescriptionConstants.MODULE, ModelType.STRING, InfinispanModel.VERSION_14_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setFlags(AttributeAccess.Flag.ALIAS);
            }
        },
        START("start", ModelType.STRING, InfinispanModel.VERSION_3_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(StartMode.LAZY.name()))
                        .setValidator(new EnumValidator<>(StartMode.class))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, InfinispanModel deprecation) {
            this.definition = this.apply(createBuilder(name, type))
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
        }

        DeprecatedAttribute(String name, InfinispanModel deprecation) {
            this.definition = new PropertiesAttributeDefinition.Builder(name)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    enum DeprecatedMetric implements org.jboss.as.clustering.controller.Attribute, AttributeTranslation, UnaryOperator<PathAddress>, Registration<ManagementResourceRegistration> {

        ACTIVATIONS(CacheActivationMetric.ACTIVATIONS),
        AVERAGE_READ_TIME(CacheMetric.AVERAGE_READ_TIME),
        AVERAGE_WRITE_TIME(CacheMetric.AVERAGE_WRITE_TIME),
        ELAPSED_TIME("elapsed-time", CacheMetric.TIME_SINCE_START),
        HIT_RATIO(CacheMetric.HIT_RATIO),
        HITS(CacheMetric.HITS),
        INVALIDATIONS(CacheInvalidationInterceptorMetric.INVALIDATIONS),
        MISSES(CacheMetric.MISSES),
        NUMBER_OF_ENTRIES(CacheMetric.NUMBER_OF_ENTRIES),
        PASSIVATIONS(CachePassivationMetric.PASSIVATIONS),
        READ_WRITE_RATIO(CacheMetric.READ_WRITE_RATIO),
        REMOVE_HITS(CacheMetric.REMOVE_HITS),
        REMOVE_MISSES(CacheMetric.REMOVE_MISSES),
        STORES("stores", CacheMetric.WRITES),
        TIME_SINCE_RESET(CacheMetric.TIME_SINCE_RESET),
        ;
        private final AttributeDefinition definition;
        private final org.jboss.as.clustering.controller.Attribute targetAttribute;

        DeprecatedMetric(Metric<?> metric) {
            this(metric.getName(), metric);
        }

        DeprecatedMetric(String name, Metric<?> metric) {
            this.targetAttribute = metric;
            this.definition = new SimpleAttributeDefinitionBuilder(name, metric.getDefinition().getType())
                    .setDeprecated(InfinispanModel.VERSION_11_0_0.getVersion())
                    .setStorageRuntime()
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public void register(ManagementResourceRegistration registration) {
            registration.registerReadOnlyAttribute(this.definition, new ReadAttributeTranslationHandler(this));
        }

        @Override
        public org.jboss.as.clustering.controller.Attribute getTargetAttribute() {
            return this.targetAttribute;
        }

        @Override
        public UnaryOperator<PathAddress> getPathAddressTransformation() {
            return this;
        }

        @Override
        public PathAddress apply(PathAddress address) {
            return address.getParent().append(CacheRuntimeResourceDefinition.pathElement(address.getLastElement().getValue()));
        }
    }

    enum FixedMetric implements OperationStepHandler, Registration<ManagementResourceRegistration> {
        CACHE_STATUS("cache-status", ModelType.STRING, new ModelNode(ComponentStatus.RUNNING.toString())),
        ;
        private final ModelNode result;
        private final AttributeDefinition definition;

        FixedMetric(String name, ModelType type, ModelNode result) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setDeprecated(InfinispanModel.VERSION_11_0_0.getVersion())
                    .setStorageRuntime()
                    .build();
            this.result = result;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) {
            context.getResult().set(this.result);
        }

        @Override
        public void register(ManagementResourceRegistration registration) {
            registration.registerReadOnlyAttribute(this.definition, this);
        }
    }

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                ;
    }

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceHandler handler;

    public CacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, CacheServiceHandler handler, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, PathElement.pathElement("cache")));
        this.configurator = configurator;
        this.handler = handler;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addAttributes(ListAttribute.class)
                .addIgnoredAttributes(EnumSet.complementOf(EnumSet.of(DeprecatedAttribute.MODULE)))
                .addAttributeTranslation(DeprecatedAttribute.MODULE, new ListAttributeTranslation(ListAttribute.MODULES))
                .addCapabilities(Capability.class)
                .addCapabilities(CLUSTERING_CAPABILITIES.values())
                .addRequiredChildren(ExpirationResourceDefinition.PATH, LockingResourceDefinition.PATH, TransactionResourceDefinition.PATH)
                .addRequiredSingletonChildren(HeapMemoryResourceDefinition.PATH, NoStoreResourceDefinition.PATH)
                ;
        new SimpleResourceRegistration(descriptor, this.handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            for (DeprecatedMetric metric : EnumSet.allOf(DeprecatedMetric.class)) {
                metric.register(registration);
            }
            for (FixedMetric metric : EnumSet.allOf(FixedMetric.class)) {
                metric.register(registration);
            }
        }

        new HeapMemoryResourceDefinition().register(registration);
        new OffHeapMemoryResourceDefinition().register(registration);

        new ExpirationResourceDefinition().register(registration);
        new LockingResourceDefinition().register(registration);
        new TransactionResourceDefinition().register(registration);

        new NoStoreResourceDefinition().register(registration);
        new CustomStoreResourceDefinition().register(registration);
        new FileStoreResourceDefinition().register(registration);
        new BinaryKeyedJDBCStoreResourceDefinition().register(registration);
        new MixedKeyedJDBCStoreResourceDefinition().register(registration);
        new StringKeyedJDBCStoreResourceDefinition().register(registration);
        new RemoteStoreResourceDefinition().register(registration);
        new HotRodStoreResourceDefinition().register(registration);

        return registration;
    }
}
