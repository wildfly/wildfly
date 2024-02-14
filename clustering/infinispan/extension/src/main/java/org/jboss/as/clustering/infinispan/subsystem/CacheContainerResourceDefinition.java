/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.clustering.controller.validation.ModuleIdentifierValidatorBuilder;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.marshall.InfinispanMarshallerFactory;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.server.service.ClusteringDefaultCacheRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class CacheContainerResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String containerName) {
        return PathElement.pathElement("cache-container", containerName);
    }

    enum Capability implements CapabilityProvider {
        CONTAINER(InfinispanRequirement.CONTAINER),
        CONFIGURATION(InfinispanRequirement.CONFIGURATION),
        KEY_AFFINITY_FACTORY(InfinispanRequirement.KEY_AFFINITY_FACTORY),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    static final Map<InfinispanCacheRequirement, org.jboss.as.clustering.controller.Capability> DEFAULT_CAPABILITIES = new EnumMap<>(InfinispanCacheRequirement.class);
    static {
        for (InfinispanCacheRequirement requirement : EnumSet.allOf(InfinispanCacheRequirement.class)) {
            DEFAULT_CAPABILITIES.put(requirement, new UnaryRequirementCapability(requirement.getDefaultRequirement()));
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DEFAULT_CACHE("default-cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false).setCapabilityReference(new CapabilityReference(DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CONFIGURATION), InfinispanCacheRequirement.CONFIGURATION, WILDCARD_PATH));
            }
        },
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(ModelNode.FALSE);
            }
        },
        MARSHALLER("marshaller", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(InfinispanMarshallerFactory.LEGACY.name()))
                        .setValidator(new ParameterValidator() {
                            private final ParameterValidator validator = EnumValidator.create(InfinispanMarshallerFactory.class);

                            @Override
                            public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                                this.validator.validateParameter(parameterName, value);
                                if (!value.isDefined() || value.equals(MARSHALLER.getDefinition().getDefaultValue())) {
                                    InfinispanLogger.ROOT_LOGGER.marshallerEnumValueDeprecated(parameterName, InfinispanMarshallerFactory.LEGACY, EnumSet.complementOf(EnumSet.of(InfinispanMarshallerFactory.LEGACY)));
                                }
                            }
                        });
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
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

    enum ListAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<StringListAttributeDefinition.Builder> {
        ALIASES("aliases"),
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

    static final Set<PathElement> REQUIRED_CHILDREN = Stream.concat(EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.CLIENT)).stream(), EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class).stream()).map(ResourceDefinitionProvider::getPathElement).collect(Collectors.toSet());
    static final Set<PathElement> REQUIRED_SINGLETON_CHILDREN = Set.of(NoTransportResourceDefinition.PATH);

    CacheContainerResourceDefinition() {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(ListAttribute.class)
                .addCapabilities(Capability.class)
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CACHE.getName()), DEFAULT_CAPABILITIES.values())
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CACHE.getName()), EnumSet.allOf(ClusteringDefaultCacheRequirement.class).stream().map(UnaryRequirementCapability::new).collect(Collectors.toList()))
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CACHE.getName()), EnumSet.allOf(SingletonDefaultCacheRequirement.class).stream().map(UnaryRequirementCapability::new).collect(Collectors.toList()))
                .addRequiredChildren(REQUIRED_CHILDREN)
                .addRequiredSingletonChildren(REQUIRED_SINGLETON_CHILDREN)
                .setResourceTransformation(CacheContainerResource::new)
                ;
        ServiceValueExecutorRegistry<EmbeddedCacheManager> managerExecutors = ServiceValueExecutorRegistry.newInstance();
        ServiceValueExecutorRegistry<Cache<?, ?>> cacheExecutors = ServiceValueExecutorRegistry.newInstance();
        ResourceServiceHandler handler = new CacheContainerServiceHandler(managerExecutors, cacheExecutors);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new CacheContainerMetricExecutor(managerExecutors), CacheContainerMetric.class).register(registration);
            new CacheRuntimeResourceDefinition(cacheExecutors).register(registration);
        }

        new JGroupsTransportResourceDefinition().register(registration);
        new NoTransportResourceDefinition().register(registration);

        for (ThreadPoolResourceDefinition pool : EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.CLIENT))) {
            pool.register(registration);
        }
        for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
            pool.register(registration);
        }

        new LocalCacheResourceDefinition(cacheExecutors).register(registration);
        new InvalidationCacheResourceDefinition(cacheExecutors).register(registration);
        new ReplicatedCacheResourceDefinition(cacheExecutors).register(registration);
        new DistributedCacheResourceDefinition(cacheExecutors).register(registration);
        new ScatteredCacheResourceDefinition(cacheExecutors).register(registration);

        return registration;
    }
}
