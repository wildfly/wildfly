/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Registers a resource definition for a cache.
 * @author Paul Ferraro
 */
public class CacheResourceDefinitionRegistrar<P extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>>> implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {

    private final RuntimeCapability<Void> cache = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE).setAllowMultipleRegistrations(true).build();

    private final CacheResourceDescription<P> description;

    public CacheResourceDefinitionRegistrar(CacheResourceDescription<P> description) {
        this.description = description;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        RuntimeCapability<Void> registryFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.REGISTRY_FACTORY).build();
        RuntimeCapability<Void> legacyRegistryFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.REGISTRY_FACTORY).build();
        RuntimeCapability<Void> serviceProviderRegistrar = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR).build();
        RuntimeCapability<Void> legacyServiceProviderRegistry = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRY).build();
        RuntimeCapability<Void> singletonServiceTargetFactory = RuntimeCapability.Builder.of(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> singletonServiceConfiguratorFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR).build();
        RuntimeCapability<Void> singletonServiceBuilderFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.singleton.SingletonServiceBuilderFactory.SERVICE_DESCRIPTOR).build();

        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.description.getPathElement(), PathElement.pathElement("cache"));
        ResourceDescriptor descriptor = this.description.apply(ResourceDescriptor.builder(resolver))
                .addCapabilities(List.of(this.cache, registryFactory, legacyRegistryFactory, serviceProviderRegistrar, legacyServiceProviderRegistry, singletonServiceTargetFactory, singletonServiceConfiguratorFactory, singletonServiceBuilderFactory))
                .requireChildResources(Set.of(ExpirationResourceDescription.INSTANCE, LockingResourceDescription.INSTANCE, TransactionResourceDescription.INSTANCE))
                .requireSingletonChildResources(Set.of(HeapMemoryResourceDescription.INSTANCE, NoStoreResourceDescription.INSTANCE))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.description, resolver, this.description.getDeprecation()).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        new ComponentResourceDefinitionRegistrar<>(HeapMemoryResourceDescription.INSTANCE).register(registration, context);
        new ComponentResourceDefinitionRegistrar<>(OffHeapMemoryResourceDescription.INSTANCE).register(registration, context);

        new ComponentResourceDefinitionRegistrar<>(ExpirationResourceDescription.INSTANCE).register(registration, context);
        new ComponentResourceDefinitionRegistrar<>(LockingResourceDescription.INSTANCE).register(registration, context);
        new ComponentResourceDefinitionRegistrar<>(TransactionResourceDescription.INSTANCE).register(registration, context);

        new ComponentResourceDefinitionRegistrar<>(NoStoreResourceDescription.INSTANCE).register(registration, context);
        new CustomStoreResourceDefinitionRegistrar<>().register(registration, context);
        new FileStoreResourceDefinitionRegistrar().register(registration, context);
        new JDBCStoreResourceDefinitionRegistrar().register(registration, context);
        new StoreResourceDefinitionRegistrar<>(RemoteStoreResourceDescription.INSTANCE).register(registration, context);
        new StoreResourceDefinitionRegistrar<>(HotRodStoreResourceDescription.INSTANCE).register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        installers.add(new CacheConfigurationServiceInstaller(config, this.description.resolve(context, model)));
        installers.add(new CacheServiceInstaller(config));

        installers.add(CacheClassLoaderServiceConfigurator.INSTANCE.configure(context, model));
        installers.add(XAResourceRecoveryServiceInstallerFactory.INSTANCE.apply(config));
        installers.add(new LazyCacheServiceInstaller(config));

        installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE.apply(config), config.resolveServiceName(LazyCacheServiceInstaller.SERVICE_DESCRIPTOR)));
        installers.add(new BinderServiceInstaller(InfinispanCacheBindingFactory.CACHE_CONFIGURATION.apply(config), config.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION)));

        Class<? extends Function<BinaryServiceConfiguration, Iterable<ServiceInstaller>>> providerClass = this.description.getCacheMode().isClustered() ? ClusteredCacheServiceInstallerProvider.class : LocalCacheServiceInstallerProvider.class;
        new ProvidedBinaryServiceInstallerProvider<>(providerClass, providerClass.getClassLoader()).apply(config).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }
}
