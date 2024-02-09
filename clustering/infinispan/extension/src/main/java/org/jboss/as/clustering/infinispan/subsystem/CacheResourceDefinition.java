/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.BinaryRequirementCapability;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.server.service.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.BinaryRequirement;
import org.wildfly.clustering.singleton.SingletonCacheRequirement;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResourceDefinition<P extends CacheServiceConfiguratorProvider> extends ChildResourceDefinition<ManagementResourceRegistration> {

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

    static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                ;
    }

    static final Set<PathElement> REQUIRED_CHILDREN = Set.of(ExpirationResourceDefinition.PATH, LockingResourceDefinition.PATH, TransactionResourceDefinition.PATH);
    static final Set<PathElement> REQUIRED_SINGLETON_CHILDREN = Set.of(HeapMemoryResourceDefinition.PATH, NoStoreResourceDefinition.PATH);

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceHandler handler;

    public CacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, CacheServiceHandler<P> handler, FunctionExecutorRegistry<Cache<?, ?>> executors) {
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
                .addCapabilities(Capability.class)
                .addCapabilities(EnumSet.allOf(ClusteringCacheRequirement.class).stream().map(BinaryRequirementCapability::new).collect(Collectors.toList()))
                .addCapabilities(EnumSet.allOf(SingletonCacheRequirement.class).stream().map(BinaryRequirementCapability::new).collect(Collectors.toList()))
                .addRequiredChildren(REQUIRED_CHILDREN)
                .addRequiredSingletonChildren(REQUIRED_SINGLETON_CHILDREN)
                ;
        new SimpleResourceRegistrar(descriptor, this.handler).register(registration);

        new HeapMemoryResourceDefinition().register(registration);
        new OffHeapMemoryResourceDefinition().register(registration);

        new ExpirationResourceDefinition().register(registration);
        new LockingResourceDefinition().register(registration);
        new TransactionResourceDefinition().register(registration);

        new NoStoreResourceDefinition().register(registration);
        new CustomStoreResourceDefinition().register(registration);
        new FileStoreResourceDefinition().register(registration);
        new JDBCStoreResourceDefinition().register(registration);
        new RemoteStoreResourceDefinition().register(registration);
        new HotRodStoreResourceDefinition().register(registration);

        return registration;
    }
}
