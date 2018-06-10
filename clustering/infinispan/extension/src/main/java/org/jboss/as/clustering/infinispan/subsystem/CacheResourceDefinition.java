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
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.Index;
import org.jboss.as.clustering.controller.BinaryRequirementCapability;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.validation.EnumValidator;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
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
        MODULE("module", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(new ModuleIdentifierValidatorBuilder().configure(builder).build());
            }
        },
        STATISTICS_ENABLED("statistics-enabled", ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(false));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(createBuilder(name, type)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @Deprecated
    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        BATCHING("batching", ModelType.BOOLEAN, InfinispanModel.VERSION_3_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(false));
            }
        },
        INDEXING("indexing", ModelType.STRING, InfinispanModel.VERSION_4_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(Index.NONE.name()))
                        .setValidator(new EnumValidator<>(Index.class))
                        ;
            }
        },
        INDEXING_PROPERTIES("indexing-properties", InfinispanModel.VERSION_4_0_0),
        JNDI_NAME("jndi-name", ModelType.STRING, InfinispanModel.VERSION_6_0_0),
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
            this.definition = this.apply(createBuilder(name, type)).setDeprecated(deprecation.getVersion()).build();
        }

        DeprecatedAttribute(String name, InfinispanModel deprecation) {
            this.definition = new PropertiesAttributeDefinition.Builder(name)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDeprecated(deprecation.getVersion())
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

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                ;
    }

    @SuppressWarnings("deprecation")
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

        BinaryMemoryResourceDefinition.buildTransformation(version, builder);
        ObjectMemoryResourceDefinition.buildTransformation(version, builder);
        OffHeapMemoryResourceDefinition.buildTransformation(version, builder);
        HotRodStoreResourceDefinition.buildTransformation(version, builder);

        LockingResourceDefinition.buildTransformation(version, builder);
        ExpirationResourceDefinition.buildTransformation(version, builder);
        TransactionResourceDefinition.buildTransformation(version, builder);

        FileStoreResourceDefinition.buildTransformation(version, builder);
        BinaryKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        MixedKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        StringKeyedJDBCStoreResourceDefinition.buildTransformation(version, builder);
        RemoteStoreResourceDefinition.buildTransformation(version, builder);
        CustomStoreResourceDefinition.buildTransformation(version, builder);
    }

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceHandler handler;

    public CacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, CacheServiceHandler handler) {
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
                .addAttributes(DeprecatedAttribute.class)
                .addCapabilities(Capability.class)
                .addCapabilities(CLUSTERING_CAPABILITIES.values())
                .addRequiredChildren(ExpirationResourceDefinition.PATH, LockingResourceDefinition.PATH, TransactionResourceDefinition.PATH)
                .addRequiredSingletonChildren(ObjectMemoryResourceDefinition.PATH, NoStoreResourceDefinition.PATH)
                ;
        new SimpleResourceRegistration(descriptor, this.handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new CacheMetricExecutor(), CacheMetric.class).register(registration);
        }

        new ObjectMemoryResourceDefinition().register(registration);
        new BinaryMemoryResourceDefinition().register(registration);
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
