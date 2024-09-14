/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.distribution.ch.impl.AffinityPartitioner;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.cache.LazyCache;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.CacheServiceInstallerFactory;
import org.wildfly.clustering.infinispan.service.ConfigurationServiceInstallerFactory;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedBinaryServiceInstallerProvider;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator, ResourceModelResolver<Map.Entry<Consumer<ConfigurationBuilder>, Stream<Consumer<RequirementServiceBuilder<?>>>>> {
    @SuppressWarnings("unchecked")
    static final BinaryServiceDescriptor<List<Module>> CACHE_MODULES = BinaryServiceDescriptor.of("org.wildfly.clustering.infinispan.cache-modules", (Class<List<Module>>) (Class<?>) List.class);

    private static final RuntimeCapability<Void> CACHE_CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE).setAllowMultipleRegistrations(true).build();
    static final RuntimeCapability<Void> CACHE_CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONFIGURATION).setAllowMultipleRegistrations(true).build();
    private static final RuntimeCapability<Void> CACHE_MODULES_CAPABILITY = RuntimeCapability.Builder.of(CACHE_MODULES).setAllowMultipleRegistrations(true).build();

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
                return builder.setElementValidator(ModuleNameValidator.INSTANCE);
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
    private final CacheMode mode;

    public CacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, CacheMode mode, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, PathElement.pathElement("cache")));
        this.configurator = configurator;
        this.mode = mode;
    }

    @SuppressWarnings({ "deprecation", "removal" })
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        RuntimeCapability<Void> registryFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.REGISTRY_FACTORY).build();
        RuntimeCapability<Void> legacyRegistryFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.REGISTRY_FACTORY).build();
        RuntimeCapability<Void> serviceProviderRegistrar = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR).build();
        RuntimeCapability<Void> legacyServiceProviderRegistry = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRY).build();
        RuntimeCapability<Void> singletonServiceTargetFactory = RuntimeCapability.Builder.of(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> singletonServiceConfiguratorFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> singletonServiceBuilderFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.SERVICE_DESCRIPTOR).build();

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addAttributes(ListAttribute.class)
                .addCapabilities(List.of(CACHE_CAPABILITY, CACHE_CONFIGURATION_CAPABILITY, CACHE_MODULES_CAPABILITY, registryFactory, legacyRegistryFactory, serviceProviderRegistrar, legacyServiceProviderRegistry, singletonServiceTargetFactory, singletonServiceConfiguratorFactory, singletonServiceBuilderFactory))
                .addRequiredChildren(REQUIRED_CHILDREN)
                .addRequiredSingletonChildren(REQUIRED_SINGLETON_CHILDREN)
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        new HeapMemoryResourceDefinition().register(registration);
        new OffHeapMemoryResourceDefinition().register(registration);

        new ExpirationResourceDefinition().register(registration);
        new LockingResourceDefinition().register(registration);
        new TransactionResourceDefinition().register(registration);

        new NoStoreResourceDefinition().register(registration);
        new CustomStoreResourceDefinition<>().register(registration);
        new FileStoreResourceDefinition().register(registration);
        new JDBCStoreResourceDefinition().register(registration);
        new RemoteStoreResourceDefinition().register(registration);
        new HotRodStoreResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();

        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = address.getLastElement().getValue();

        BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(containerName, cacheName);

        List<ResourceServiceInstaller> installers = new LinkedList<>();

        if (model.hasDefined(ListAttribute.MODULES.getName())) {
            installers.add(new ModulesServiceConfigurator(CACHE_MODULES_CAPABILITY, ListAttribute.MODULES.getDefinition(), List.of()).configure(context, model));
        } else {
            installers.add(CapabilityServiceInstaller.builder(CACHE_MODULES_CAPABILITY, configuration.getServiceDependency(CacheContainerResourceDefinition.CACHE_CONTAINER_MODULES)).build());
        }

        Map.Entry<Consumer<ConfigurationBuilder>, Stream<Consumer<RequirementServiceBuilder<?>>>> entry = this.resolve(context, model);
        installers.add(new ConfigurationServiceInstallerFactory(entry.getKey(), entry.getValue().collect(Collectors.toList())).apply(configuration));

        installers.add(CacheServiceInstallerFactory.INSTANCE.apply(configuration));

        // Lazy cache service for use by injected cache instances
        ServiceName lazyCacheServiceName = CACHE_CAPABILITY.getCapabilityServiceName(address).append("lazy");
        ServiceDependency<EmbeddedCacheManager> container = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER);
        Supplier<Cache<?, ?>> factory = new Supplier<>() {
            @Override
            public Cache<?, ?> get() {
                return new LazyCache<>(container.get(), cacheName);
            }
        };
        installers.add(ServiceInstaller.builder(factory).provides(lazyCacheServiceName).requires(List.of(container, configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE))).build());
        installers.add(new BinderServiceInstaller(InfinispanBindingFactory.createCacheConfigurationBinding(configuration), CacheResourceDefinition.CACHE_CONFIGURATION_CAPABILITY.getCapabilityServiceName(address)));
        installers.add(new BinderServiceInstaller(InfinispanBindingFactory.createCacheBinding(configuration), lazyCacheServiceName));

        Class<? extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>>> providerClass = this.mode.isClustered() ? ClusteredCacheServiceInstallerProvider.class : LocalCacheServiceInstallerProvider.class;
        new ProvidedBinaryServiceInstallerProvider<>(providerClass, providerClass.getClassLoader()).apply(configuration).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }

    @Override
    public MapEntry<Consumer<ConfigurationBuilder>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String containerName = address.getParent().getLastElement().getValue();
        String cacheName = address.getLastElement().getValue();
        CacheMode mode = this.mode;

        boolean statisticsEnabled = Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        ServiceDependency<MemoryConfiguration> memory = ServiceDependency.on(MemoryResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        ServiceDependency<ExpirationConfiguration> expiration = ServiceDependency.on(ExpirationResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        ServiceDependency<LockingConfiguration> locking = ServiceDependency.on(LockingResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        ServiceDependency<PersistenceConfiguration> persistence = ServiceDependency.on(StoreResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        ServiceDependency<TransactionConfiguration> transaction = ServiceDependency.on(TransactionResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);

        return MapEntry.of(new Consumer<>() {
            @Override
            public void accept(ConfigurationBuilder builder) {
                TransactionConfiguration tx = transaction.get();

                builder.clustering().cacheMode(mode).hash().keyPartitioner(new AffinityPartitioner());
                builder.memory().read(memory.get());
                builder.expiration().read(expiration.get());
                builder.locking().read(locking.get());
                builder.persistence().read(persistence.get());
                builder.transaction().read(tx);
                builder.statistics().enabled(statisticsEnabled);

                // Configure invocation batching based on transaction configuration
                try {
                    builder.invocationBatching().enable(tx.transactionMode().isTransactional() && (tx.transactionManagerLookup().getTransactionManager() == EmbeddedTransactionManager.getInstance()));
                } catch (Exception e) {
                    throw new CacheConfigurationException(e);
                }
            }
        }, Stream.of(memory, expiration, locking, persistence, transaction));
    }
}
