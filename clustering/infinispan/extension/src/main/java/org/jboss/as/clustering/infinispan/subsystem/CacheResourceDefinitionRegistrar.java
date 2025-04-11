/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.TransactionConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.controller.ModuleListAttributeDefinition;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.wildfly.clustering.infinispan.service.CacheConfigurationServiceInstaller;
import org.wildfly.clustering.infinispan.service.CacheServiceInstaller;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBinaryServiceInstallerProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Registers a resource definition for a cache configuration.
 * @author Paul Ferraro
 */
public class CacheResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<ServiceDependency<ConfigurationBuilder>>, UnaryOperator<ResourceDescriptor.Builder> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONFIGURATION).setAllowMultipleRegistrations(true).build();

    static final BinaryServiceDescriptor<ClassLoader> CLASS_LOADER = BinaryServiceDescriptor.of(InfinispanServiceDescriptor.CACHE_CONFIGURATION.getName() + ".loader", ClassLoader.class);

    static final ResourceCapabilityReference<GlobalConfiguration> CACHE_CONTAINER_CONFIGURATION = ResourceCapabilityReference.builder(CAPABILITY, InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION).withRequirementNameResolver(UnaryCapabilityNameResolver.PARENT).build();

    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();
    static final ModuleListAttributeDefinition MODULES = new ModuleListAttributeDefinition.Builder().setRequired(false).build();

    private final CacheResourceRegistration registration;

    public CacheResourceDefinitionRegistrar(CacheResourceRegistration registration) {
        this.registration = registration;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        RuntimeCapability<Void> cache = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> registryFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.REGISTRY_FACTORY).build();
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> legacyRegistryFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.REGISTRY_FACTORY).build();
        RuntimeCapability<Void> serviceProviderRegistrar = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR).build();
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> legacyServiceProviderRegistry = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRY).build();
        RuntimeCapability<Void> singletonServiceTargetFactory = RuntimeCapability.Builder.of(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR).build();
        @SuppressWarnings("removal")
        RuntimeCapability<Void> singletonServiceConfiguratorFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR).build();
        @SuppressWarnings("removal")
        RuntimeCapability<Void> singletonServiceBuilderFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.SERVICE_DESCRIPTOR).build();

        return builder.addAttributes(List.of(STATISTICS_ENABLED, MODULES))
            .addCapabilities(List.of(CAPABILITY, cache, registryFactory, legacyRegistryFactory, serviceProviderRegistrar, legacyServiceProviderRegistry, singletonServiceTargetFactory, singletonServiceConfiguratorFactory, singletonServiceBuilderFactory))
            .addResourceCapabilityReference(CACHE_CONTAINER_CONFIGURATION)
            .requireChildResources(Set.of(ComponentResourceRegistration.EXPIRATION, ComponentResourceRegistration.LOCKING, ComponentResourceRegistration.TRANSACTION))
            .requireSingletonChildResources(Set.of(MemoryResourceRegistration.HEAP, StoreResourceRegistration.NONE))
            .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
            ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement(), PathElement.pathElement("cache"));
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.registration, resolver, this.registration.getDeprecation()).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        new HeapMemoryResourceDefinitionRegistrar().register(registration, context);
        new OffHeapMemoryResourceDefinitionRegistrar().register(registration, context);

        new ExpirationResourceDefinitionRegistrar().register(registration, context);
        new LockingResourceDefinitionRegistrar().register(registration, context);
        new TransactionResourceDefinitionRegistrar().register(registration, context);

        new NoStoreResourceDefinitionRegistrar().register(registration, context);
        new CustomStoreResourceDefinitionRegistrar<>().register(registration, context);
        new FileStoreResourceDefinitionRegistrar().register(registration, context);
        new JDBCStoreResourceDefinitionRegistrar().register(registration, context);
        new RemoteStoreResourceDefinitionRegistrar().register(registration, context);
        new HotRodStoreResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        installers.add(new CacheConfigurationServiceInstaller(config, this.resolve(context, model)));
        installers.add(new CacheServiceInstaller(config));
        installers.add(new LazyCacheServiceInstaller(config));

        ServiceDependency<ClassLoader> loader = MODULES.resolve(context, model).combine(CACHE_CONTAINER_CONFIGURATION.resolve(context, model), new BiFunction<>() {
            @Override
            public ClassLoader apply(List<Module> modules, GlobalConfiguration global) {
                if (modules.isEmpty()) {
                    return global.classLoader();
                }
                if (modules.size() == 1) {
                    return modules.get(0).getClassLoader();
                }
                return new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toUnmodifiableList()));
            }
        });
        installers.add(ServiceInstaller.builder(loader).provides(config.resolveServiceName(CLASS_LOADER)).build());

        installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE.apply(config), config.resolveServiceName(LazyCacheServiceInstaller.SERVICE_DESCRIPTOR)));
        installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE_CONFIGURATION.apply(config), config.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION)));

        Class<? extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>>> providerClass = this.registration.getCacheMode().isClustered() ? ClusteredCacheServiceInstallerProvider.class : LocalCacheServiceInstallerProvider.class;
        new ProvidedBinaryServiceInstallerProvider<>(providerClass, providerClass.getClassLoader()).apply(config).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());

        CacheMode mode = this.registration.getCacheMode();
        boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);
        ServiceDependency<ExpirationConfiguration> expiration = config.getServiceDependency(ExpirationResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
        ServiceDependency<MemoryConfiguration> memory = config.getServiceDependency(MemoryResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
        ServiceDependency<LockingConfiguration> locking = config.getServiceDependency(LockingResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
        ServiceDependency<PersistenceConfiguration> persistence = config.getServiceDependency(PersistenceResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
        ServiceDependency<TransactionConfiguration> transaction = config.getServiceDependency(TransactionResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);

        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                memory.accept(builder);
                expiration.accept(builder);
                locking.accept(builder);
                transaction.accept(builder);
                persistence.accept(builder);
            }

            @Override
            public ConfigurationBuilder get() {
                ConfigurationBuilder builder = new ConfigurationBuilder();

                builder.clustering().cacheMode(mode);
                builder.memory().read(memory.get());
                builder.expiration().read(expiration.get());
                builder.locking().read(locking.get());
                builder.persistence().read(persistence.get());
                builder.transaction().read(transaction.get());
                builder.statistics().enabled(statisticsEnabled);

                try {
                    // Configure invocation batching based on transaction configuration
                    TransactionConfigurationBuilder tx = builder.transaction();
                    builder.invocationBatching().enable(tx.transactionMode().isTransactional() && (tx.transactionManagerLookup().getTransactionManager() == EmbeddedTransactionManager.getInstance()));
                } catch (Exception e) {
                    throw new CacheException(e);
                }
                return builder;
            }
        };
    }
}
