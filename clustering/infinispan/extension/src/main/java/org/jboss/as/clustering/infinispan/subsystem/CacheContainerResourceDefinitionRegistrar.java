/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBinaryServiceInstallerProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Registers a cache container resource definition.
 * @author Paul Ferraro
 */
public class CacheContainerResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, BiPredicate<OperationContext, Resource> {

    enum DefaultCacheCapability implements Supplier<RuntimeCapability<?>> {
        CACHE(InfinispanServiceDescriptor.DEFAULT_CACHE),
        CACHE_CONFIGURATION(InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION),
        ;
        private final RuntimeCapability<Void> capability;

        DefaultCacheCapability(UnaryServiceDescriptor<?> descriptor) {
            this.capability = RuntimeCapability.Builder.of(descriptor).build();
        }

        @Override
        public RuntimeCapability<Void> get() {
            return this.capability;
        }
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        RuntimeCapability<Void> defaultRegistryFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_REGISTRY_FACTORY).build();
        RuntimeCapability<Void> defaultLegacyRegistryFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_REGISTRY_FACTORY).build();
        RuntimeCapability<Void> defaultServiceProviderRegistrar = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_SERVICE_PROVIDER_REGISTRAR).build();
        RuntimeCapability<Void> defaultLegacyServiceProviderRegistry = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_SERVICE_PROVIDER_REGISTRY).build();
        RuntimeCapability<Void> defaultSingletonServiceTargetFactory = RuntimeCapability.Builder.of(SingletonServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> defaultSingletonServiceConfiguratorFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.DEFAULT_SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> defaultSingletonServiceBuilderFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.DEFAULT_SERVICE_DESCRIPTOR).build();

        ServiceValueExecutorRegistry<EmbeddedCacheManager> containerExecutors = ServiceValueExecutorRegistry.newInstance();
        ServiceValueExecutorRegistry<Cache<?, ?>> cacheExecutors = ServiceValueExecutorRegistry.newInstance();

        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(CacheContainerResourceDescription.INSTANCE.getPathElement());
        ResourceDescriptor descriptor = CacheContainerResourceDescription.INSTANCE.apply(ResourceDescriptor.builder(resolver))
                .addCapability(CacheContainerServiceConfigurator.CAPABILITY)
                .provideCapabilities(EnumSet.allOf(DefaultCacheCapability.class), this)
                .addCapabilities(List.of(defaultRegistryFactory, defaultLegacyRegistryFactory, defaultServiceProviderRegistrar, defaultLegacyServiceProviderRegistry, defaultSingletonServiceTargetFactory, defaultSingletonServiceConfiguratorFactory, defaultSingletonServiceBuilderFactory), this)
                .requireChildResources(EnumSet.allOf(ThreadPool.class))
                .requireChildResources(EnumSet.allOf(ScheduledThreadPool.class))
                .requireSingletonChildResource(NoTransportResourceDescription.INSTANCE)
                .withResourceTransformation(CacheContainerResource::new)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(ResourceServiceConfigurator.combine(CacheContainerResourceDescription.INSTANCE, new CacheContainerServiceConfigurator(containerExecutors, cacheExecutors), this)))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(CacheContainerResourceDescription.INSTANCE, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new MetricOperationStepHandler<>(new CacheContainerMetricExecutor(containerExecutors), CacheContainerMetric.class).register(registration);
            new CacheRuntimeResourceDefinitionRegistrar(cacheExecutors).register(registration, context);
        }

        for (TransportResourceDescription transport : List.of(JGroupsTransportResourceDescription.INSTANCE, NoTransportResourceDescription.INSTANCE)) {
            new TransportResourceDefinitionRegistrar(transport).register(registration, context);
        }

        for (ThreadPool pool : EnumSet.allOf(ThreadPool.class)) {
            new ComponentResourceDefinitionRegistrar<>(pool).register(registration, context);
        }
        for (ScheduledThreadPool pool : EnumSet.allOf(ScheduledThreadPool.class)) {
            new ComponentResourceDefinitionRegistrar<>(pool).register(registration, context);
        }

        new CacheResourceDefinitionRegistrar<>(LocalCacheResourceDescription.INSTANCE).register(registration, context);
        new CacheResourceDefinitionRegistrar<>(InvalidationCacheResourceDescription.INSTANCE).register(registration, context);
        new SharedStateCacheResourceDefinitionRegistrar(ReplicatedCacheResourceDescription.INSTANCE, cacheExecutors).register(registration, context);
        new SharedStateCacheResourceDefinitionRegistrar(DistributedCacheResourceDescription.INSTANCE, cacheExecutors).register(registration, context);
        new SharedStateCacheResourceDefinitionRegistrar(ScatteredCacheResourceDescription.INSTANCE, cacheExecutors).register(registration, context);

        return registration;
    }

    @Override
    public boolean test(OperationContext context, Resource resource) {
        return resource.getModel().hasDefined(CacheContainerResourceDescription.DEFAULT_CACHE.getName());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        List<ResourceServiceInstaller> installers = new LinkedList<>();

        installers.add(new BinderServiceInstaller(InfinispanCacheContainerBindingFactory.EMBEDDED.apply(name), context.getCapabilityServiceName(InfinispanServiceDescriptor.CACHE_CONTAINER, name)));

        String defaultCache = CacheContainerResourceDescription.DEFAULT_CACHE.resolveModelAttribute(context, model).asString(null);
        if (defaultCache != null) {
            BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(name, defaultCache);
            installers.add(CapabilityServiceInstaller.builder(DefaultCacheCapability.CACHE_CONFIGURATION.get(), configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION)).build());
            installers.add(CapabilityServiceInstaller.builder(DefaultCacheCapability.CACHE.get(), configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE)).build());

            // Install bindings for default cache
            if (!defaultCache.equals(ModelDescriptionConstants.DEFAULT)) {
                BinaryServiceConfiguration defaultConfiguration = configuration.withChildName(ModelDescriptionConstants.DEFAULT);
                installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE.apply(defaultConfiguration), configuration.resolveServiceName(LazyCacheServiceInstaller.SERVICE_DESCRIPTOR)));
                installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE_CONFIGURATION.apply(defaultConfiguration), configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION)));
            }

            new ProvidedBinaryServiceInstallerProvider<>(DefaultCacheServiceInstallerProvider.class, DefaultCacheServiceInstallerProvider.class.getClassLoader()).apply(configuration).forEach(installers::add);
        }

        return ResourceServiceInstaller.combine(installers);
    }
}
