/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.cache.infinispan.embedded.lifecycle.WildFlyClusteringModuleLifecycle;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBinaryServiceInstallerProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class CacheContainerResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String containerName) {
        return PathElement.pathElement("cache-container", containerName);
    }

    @SuppressWarnings("unchecked")
    static final UnaryServiceDescriptor<List<Module>> CACHE_CONTAINER_MODULES = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.cache-container-modules", (Class<List<Module>>) (Class<?>) List.class);

    private static final RuntimeCapability<Void> CACHE_CONTAINER_CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONTAINER).build();
    private static final RuntimeCapability<Void> CACHE_CONTAINER_CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION).build();
    private static final RuntimeCapability<Void> CACHE_CONTAINER_MODULES_CAPABILITY = RuntimeCapability.Builder.of(CACHE_CONTAINER_MODULES).build();

    private static final RuntimeCapability<Void> DEFAULT_CACHE_CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.DEFAULT_CACHE).build();
    private static final RuntimeCapability<Void> DEFAULT_CACHE_CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DEFAULT_CACHE("default-cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false).setCapabilityReference(CapabilityReference.builder(DEFAULT_CACHE_CONFIGURATION_CAPABILITY, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentPath(WILDCARD_PATH).build());
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
                return builder.setElementValidator(ModuleNameValidator.INSTANCE);
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

    static final Set<PathElement> REQUIRED_CHILDREN = Stream.concat(EnumSet.allOf(ThreadPoolResourceDefinition.class).stream(), EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class).stream()).map(ResourceDefinitionProvider::getPathElement).collect(Collectors.toSet());
    static final Set<PathElement> REQUIRED_SINGLETON_CHILDREN = Set.of(NoTransportResourceDefinition.PATH);

    private final ServiceValueExecutorRegistry<EmbeddedCacheManager> containerRegistry = ServiceValueExecutorRegistry.newInstance();
    private final ServiceValueExecutorRegistry<Cache<?, ?>> cacheRegistry = ServiceValueExecutorRegistry.newInstance();

    CacheContainerResourceDefinition() {
        super(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @SuppressWarnings({ "deprecation", "removal" })
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        RuntimeCapability<Void> defaultRegistryFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_REGISTRY_FACTORY).build();
        RuntimeCapability<Void> defaultLegacyRegistryFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_REGISTRY_FACTORY).build();
        RuntimeCapability<Void> defaultServiceProviderRegistrar = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_SERVICE_PROVIDER_REGISTRAR).build();
        RuntimeCapability<Void> defaultLegacyServiceProviderRegistry = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_SERVICE_PROVIDER_REGISTRY).build();
        RuntimeCapability<Void> defaultSingletonServiceTargetFactory = RuntimeCapability.Builder.of(SingletonServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> defaultSingletonServiceConfiguratorFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.DEFAULT_SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> defaultSingletonServiceBuilderFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.DEFAULT_SERVICE_DESCRIPTOR).build();

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(ListAttribute.class)
                .addCapabilities(List.of(CACHE_CONTAINER_CAPABILITY, CACHE_CONTAINER_CONFIGURATION_CAPABILITY, CACHE_CONTAINER_MODULES_CAPABILITY))
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CACHE.getName()), List.of(DEFAULT_CACHE_CAPABILITY, DEFAULT_CACHE_CONFIGURATION_CAPABILITY, defaultRegistryFactory, defaultLegacyRegistryFactory, defaultServiceProviderRegistrar, defaultLegacyServiceProviderRegistry, defaultSingletonServiceTargetFactory, defaultSingletonServiceConfiguratorFactory, defaultSingletonServiceBuilderFactory))
                .addRequiredChildren(REQUIRED_CHILDREN)
                .addRequiredSingletonChildren(REQUIRED_SINGLETON_CHILDREN)
                .setResourceTransformation(CacheContainerResource::new)
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new CacheContainerMetricExecutor(this.containerRegistry), CacheContainerMetric.class).register(registration);
            new CacheRuntimeResourceDefinition(this.cacheRegistry).register(registration);
        }

        new JGroupsTransportResourceDefinition().register(registration);
        new NoTransportResourceDefinition().register(registration);

        for (ThreadPoolResourceDefinition pool : EnumSet.allOf(ThreadPoolResourceDefinition.class)) {
            pool.register(registration);
        }
        for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
            pool.register(registration);
        }

        new LocalCacheResourceDefinition(this.cacheRegistry).register(registration);
        new InvalidationCacheResourceDefinition(this.cacheRegistry).register(registration);
        new ReplicatedCacheResourceDefinition(this.cacheRegistry).register(registration);
        new DistributedCacheResourceDefinition(this.cacheRegistry).register(registration);
        new ScatteredCacheResourceDefinition(this.cacheRegistry).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();
        List<ResourceServiceInstaller> installers = new LinkedList<>();

        installers.add(new ModulesServiceConfigurator(CACHE_CONTAINER_MODULES_CAPABILITY, ListAttribute.MODULES.getDefinition(), List.of(Module.forClass(WildFlyClusteringModuleLifecycle.class))).configure(context, model));
        installers.add(new GlobalConfigurationServiceConfigurator(CACHE_CONTAINER_CONFIGURATION_CAPABILITY).configure(context, model));
        installers.add(this.containerRegistry.capture(ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER, name)));
        installers.add(new CacheContainerServiceConfigurator(CACHE_CONTAINER_CAPABILITY, this.cacheRegistry).configure(context, model));
        installers.add(new BinderServiceInstaller(InfinispanBindingFactory.createCacheContainerBinding(name), context.getCapabilityServiceName(InfinispanServiceDescriptor.CACHE_CONTAINER, name)));

        String defaultCache = Attribute.DEFAULT_CACHE.resolveModelAttribute(context, model).asStringOrNull();
        if (defaultCache != null) {
            BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(name, defaultCache);
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CACHE_CONFIGURATION_CAPABILITY, configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION)).build());
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CACHE_CAPABILITY, configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE)).build());

            if (!defaultCache.equals(ModelDescriptionConstants.DEFAULT)) {
                ServiceName lazyCacheServiceName = configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE).append("lazy");
                BinaryServiceConfiguration defaultConfiguration = configuration.withChildName(ModelDescriptionConstants.DEFAULT);
                installers.add(new BinderServiceInstaller(InfinispanBindingFactory.createCacheBinding(defaultConfiguration), lazyCacheServiceName));
                installers.add(new BinderServiceInstaller(InfinispanBindingFactory.createCacheConfigurationBinding(defaultConfiguration), DEFAULT_CACHE_CONFIGURATION_CAPABILITY.getCapabilityServiceName(address)));
            }

            new ProvidedBinaryServiceInstallerProvider<>(DefaultCacheServiceInstallerProvider.class, DefaultCacheServiceInstallerProvider.class.getClassLoader()).apply(context.getCapabilityServiceSupport(), configuration).forEach(installers::add);
        }

        return ResourceServiceInstaller.combine(installers);
    }
}
