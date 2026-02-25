/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServer;

import org.infinispan.Cache;
import org.infinispan.commons.jmx.MBeanServerLookup;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.UncleanShutdownAction;
import org.infinispan.configuration.internal.PrivateGlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.as.clustering.controller.MBeanServerResolver;
import org.jboss.as.clustering.infinispan.jmx.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.cache.infinispan.embedded.lifecycle.WildFlyClusteringModuleLifecycle;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBinaryServiceInstallerProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.EnumAttributeDefinition;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ModuleListAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.StatisticsEnabledAttributeDefinition;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Registers a cache container resource definition.
 * @author Paul Ferraro
 */
public class CacheContainerResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, BiPredicate<OperationContext, Resource> {

    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("cache-container"));

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION).build();

    static final CapabilityReferenceAttributeDefinition<Configuration> DEFAULT_CACHE = new CapabilityReferenceAttributeDefinition.Builder<>("default-cache", CapabilityReference.builder(CAPABILITY, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentPath(REGISTRATION.getPathElement()).build()).setRequired(false).build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();
    static final EnumAttributeDefinition<InfinispanMarshallerFactory> MARSHALLER = EnumAttributeDefinition.nameBuilder("marshaller", InfinispanMarshallerFactory.class)
            .setDefaultValue(InfinispanMarshallerFactory.LEGACY)
            .setCorrector(InfinispanMarshallerFactory.CORRECTOR)
            .build();
    static final StringListAttributeDefinition ALIASES = new StringListAttributeDefinition.Builder("aliases").setRequired(false).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();
    static final ModuleListAttributeDefinition MODULES = new ModuleListAttributeDefinition.Builder().setRequired(false).setDefaultValue(Module.forClass(WildFlyClusteringModuleLifecycle.class)).build();

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
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> defaultLegacyRegistryFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_REGISTRY_FACTORY).build();
        RuntimeCapability<Void> defaultServiceProviderRegistrar = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_SERVICE_PROVIDER_REGISTRAR).build();
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> defaultLegacyServiceProviderRegistry = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_SERVICE_PROVIDER_REGISTRY).build();
        RuntimeCapability<Void> defaultSingletonServiceTargetFactory = RuntimeCapability.Builder.of(SingletonServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR).build();
        @SuppressWarnings("removal")
        RuntimeCapability<Void> defaultSingletonServiceConfiguratorFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.DEFAULT_SERVICE_DESCRIPTOR).build();
        @SuppressWarnings("removal")
        RuntimeCapability<Void> defaultSingletonServiceBuilderFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.DEFAULT_SERVICE_DESCRIPTOR).build();

        ServiceValueExecutorRegistry<EmbeddedCacheManager> containerExecutors = ServiceValueExecutorRegistry.newInstance();
        ServiceValueExecutorRegistry<Cache<?, ?>> cacheExecutors = ServiceValueExecutorRegistry.newInstance();

        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(CacheContainerResourceDefinitionRegistrar.REGISTRATION.getPathElement());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(List.of(DEFAULT_CACHE, STATISTICS_ENABLED, MARSHALLER, ALIASES, MODULES))
                .addCapabilities(List.of(CAPABILITY, CacheContainerServiceConfigurator.CAPABILITY))
                .provideCapabilities(EnumSet.allOf(DefaultCacheCapability.class), this)
                .addCapabilities(List.of(defaultRegistryFactory, defaultLegacyRegistryFactory, defaultServiceProviderRegistrar, defaultLegacyServiceProviderRegistry, defaultSingletonServiceTargetFactory, defaultSingletonServiceConfiguratorFactory, defaultSingletonServiceBuilderFactory), this)
                .requireChildResources(EnumSet.allOf(ThreadPool.class))
                .requireChildResources(EnumSet.allOf(ScheduledThreadPool.class))
                .requireSingletonChildResource(TransportResourceRegistration.NONE)
                .withResourceTransformation(CacheContainerResource::new)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(ResourceServiceConfigurator.combine(this, new CacheContainerServiceConfigurator(containerExecutors, cacheExecutors))))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(REGISTRATION, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new MetricOperationStepHandler<>(new CacheContainerMetricExecutor(containerExecutors), CacheContainerMetric.class).register(registration);
            new CacheRuntimeResourceDefinitionRegistrar(cacheExecutors).register(registration, context);
        }

        new NoTransportResourceDefinitionRegistrar().register(registration, context);
        new JGroupsTransportResourceDefinitionRegistrar().register(registration, context);

        for (ThreadPool pool : EnumSet.allOf(ThreadPool.class)) {
            new ThreadPoolResourceDefinitionRegistrar(pool).register(registration, context);
        }
        for (ScheduledThreadPool pool : EnumSet.allOf(ScheduledThreadPool.class)) {
            new ScheduledThreadPoolResourceDefinitionRegistrar(pool).register(registration, context);
        }

        new LocalCacheResourceDefinitionRegistrar().register(registration, context);
        new InvalidationCacheResourceDefinitionRegistrar().register(registration, context);
        new ReplicatedCacheResourceDefinitionRegistrar(cacheExecutors).register(registration, context);
        new DistributedCacheResourceDefinitionRegistrar(cacheExecutors).register(registration, context);
        new ScatteredCacheResourceDefinitionRegistrar(cacheExecutors).register(registration, context);

        return registration;
    }

    @Override
    public boolean test(OperationContext context, Resource resource) {
        return resource.getModel().hasDefined(DEFAULT_CACHE.getName());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        List<ResourceServiceInstaller> installers = new LinkedList<>();

        List<String> aliases = ALIASES.resolveModelAttribute(context, model).asListOrEmpty().stream().map(ModelNode::asString).toList();
        String defaultCacheName = DEFAULT_CACHE.resolveModelAttribute(context, model).asStringOrNull();
        boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);
        InfinispanMarshallerFactory marshallerFactory = MARSHALLER.resolve(context, model);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        ServiceDependency<List<Module>> containerModules = MODULES.resolve(context, model);
        ServiceDependency<TransportConfiguration> transport = ServiceDependency.on(TransportResourceDefinitionRegistrar.SERVICE_DESCRIPTOR, name);
        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        ServiceDependency<MBeanServer> mbeanServer = new MBeanServerResolver(CAPABILITY).resolve(context, model);
        Map<ThreadPool, ServiceDependency<ThreadPoolConfiguration>> pools = new EnumMap<>(ThreadPool.class);
        Map<ScheduledThreadPool, ServiceDependency<ThreadPoolConfiguration>> scheduledPools = new EnumMap<>(ScheduledThreadPool.class);
        for (ThreadPool pool : EnumSet.allOf(ThreadPool.class)) {
            pools.put(pool, ServiceDependency.on(pool.getServiceDescriptor(), name));
        }
        for (ScheduledThreadPool pool : EnumSet.allOf(ScheduledThreadPool.class)) {
            scheduledPools.put(pool, ServiceDependency.on(pool.getServiceDescriptor(), name));
        }
        Supplier<GlobalConfiguration> factory = new Supplier<>() {
            @Override
            public GlobalConfiguration get() {
                GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
                builder.cacheManagerName(name)
                        .defaultCacheName(defaultCacheName)
                        .cacheContainer().statistics(statisticsEnabled)
                ;

                builder.transport().read(transport.get());

                List<Module> modules = containerModules.get();
                Marshaller marshaller = marshallerFactory.apply(loader.get(), modules);
                InfinispanLogger.ROOT_LOGGER.debugf("%s cache-container will use %s", name, marshaller.getClass().getName());
                // Register dummy serialization context initializer, to bypass service loading in org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl
                // Otherwise marshaller auto-detection will not work
                builder.serialization().marshaller(marshaller).addContextInitializer(new SerializationContextInitializer() {
                    @Override
                    public void registerMarshallers(SerializationContext context) {
                    }

                    @Override
                    public void registerSchema(SerializationContext context) {
                    }
                });

                ClassLoader loader = modules.size() > 1 ? new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toList())) : modules.get(0).getClassLoader();
                builder.classLoader(loader);

                builder.blockingThreadPool().read(pools.get(ThreadPool.BLOCKING).get());
                builder.listenerThreadPool().read(pools.get(ThreadPool.LISTENER).get());
                builder.nonBlockingThreadPool().read(pools.get(ThreadPool.NON_BLOCKING).get());
                builder.expirationThreadPool().read(scheduledPools.get(ScheduledThreadPool.EXPIRATION).get());

                builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);
                // Disable native Micrometer registration - we register metrics via management model
                builder.metrics().gauges(false).histograms(false).accurateSize(true);

                MBeanServerLookup mbeanServerProvider = Optional.ofNullable(mbeanServer.get()).map(MBeanServerProvider::new).orElse(null);
                builder.jmx().domain("org.wildfly.clustering.infinispan")
                        .mBeanServerLookup(mbeanServerProvider)
                        .enabled(mbeanServerProvider != null)
                        ;

                // Disable triangle algorithm for transactional distributed caches - we optimize for originator as primary owner
                // Now that managed cache configurations always define a cache encoding, this should no longer be problematic for Hibernate 2LC interceptors
                // See ISPN-12252 for details
                builder.addModule(PrivateGlobalConfigurationBuilder.class).serverMode(true);

                String path = InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getName() + File.separatorChar + name;
                builder.globalState().enable()
                        .configurationStorage(ConfigurationStorage.VOLATILE)
                        .persistentLocation(path, environment.get().getServerDataDir().getPath())
                        .temporaryLocation(path, environment.get().getServerTempDir().getPath())
                        .uncleanShutdownAction(UncleanShutdownAction.PURGE);
                return builder.build();
            }
        };
        CapabilityServiceInstaller.Builder<GlobalConfiguration, GlobalConfiguration> builder = CapabilityServiceInstaller.builder(CAPABILITY, factory);
        for (String alias : aliases) {
            builder.provides(ServiceNameFactory.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION, alias));
        }
        installers.add(builder.blocking()
            .requires(List.of(mbeanServer, loader, containerModules, transport, environment))
            .requires(pools.values())
            .requires(scheduledPools.values())
            .startWhen(StartWhen.AVAILABLE)
            .build());

        String defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asString(null);
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
