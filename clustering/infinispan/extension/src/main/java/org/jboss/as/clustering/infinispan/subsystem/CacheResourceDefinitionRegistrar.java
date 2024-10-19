/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.TransactionConfigurationBuilder;
import org.infinispan.distribution.ch.impl.AffinityPartitioner;
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
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.CacheConfigurationServiceInstaller;
import org.wildfly.clustering.infinispan.service.CacheServiceInstaller;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBinaryServiceInstallerProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Registers a resource definition for a cache.
 * @author Paul Ferraro
 */
public class CacheResourceDefinitionRegistrar<P extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>>> implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<ServiceDependency<ConfigurationBuilder>> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONFIGURATION).setAllowMultipleRegistrations(true).build();

    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();
    static final ModuleListAttributeDefinition MODULES = new ModuleListAttributeDefinition.Builder().setRequired(false).build();

    interface Configurator<P extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>>> extends UnaryOperator<ResourceDescriptor.Builder> {
        CacheResourceRegistration getResourceRegistration();

        Class<P> getServiceInstallerProviderClass();

        default InfinispanSubsystemModel getDeprecation() {
            return null;
        }

        @Override
        default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
            return builder;
        }
    }

    private final Configurator<P> configurator;

    public CacheResourceDefinitionRegistrar(Configurator<P> configurator) {
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
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

        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.configurator.getResourceRegistration().getPathElement(), PathElement.pathElement("cache"));
        ResourceDescriptor descriptor = this.configurator.apply(ResourceDescriptor.builder(resolver))
                .addAttributes(List.of(STATISTICS_ENABLED, MODULES))
                .addCapabilities(List.of(CAPABILITY, cache, registryFactory, legacyRegistryFactory, serviceProviderRegistrar, legacyServiceProviderRegistry, singletonServiceTargetFactory, singletonServiceConfiguratorFactory, singletonServiceBuilderFactory))
                .requireChildResources(Set.of(ComponentResourceRegistration.EXPIRATION, ComponentResourceRegistration.LOCKING, ComponentResourceRegistration.TRANSACTION))
                .requireSingletonChildResources(Set.of(MemoryResourceRegistration.HEAP, StoreResourceRegistration.NONE))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.configurator.getResourceRegistration(), resolver, this.configurator.getDeprecation()).build());

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

        installers.add(CacheClassLoaderServiceConfigurator.INSTANCE.configure(context, model));
        installers.add(XAResourceRecoveryServiceInstallerFactory.INSTANCE.apply(config));
        installers.add(new LazyCacheServiceInstaller(config));

        installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE.apply(config), config.resolveServiceName(LazyCacheServiceInstaller.SERVICE_DESCRIPTOR)));
        installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE_CONFIGURATION.apply(config), config.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION)));

        Class<? extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>>> providerClass = this.configurator.getResourceRegistration().getCacheMode().isClustered() ? ClusteredCacheServiceInstallerProvider.class : LocalCacheServiceInstallerProvider.class;
        new ProvidedBinaryServiceInstallerProvider<>(providerClass, providerClass.getClassLoader()).apply(config).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());

        CacheMode mode = this.configurator.getResourceRegistration().getCacheMode();
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

                builder.clustering().cacheMode(mode).hash().keyPartitioner(new AffinityPartitioner());
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
