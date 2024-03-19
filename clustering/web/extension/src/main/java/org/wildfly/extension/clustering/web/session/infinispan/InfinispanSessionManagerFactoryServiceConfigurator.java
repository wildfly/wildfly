/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web.session.infinispan;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.CompositeServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.infinispan.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.server.NodeFactory;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.ProvidedCacheServiceConfigurator;
import org.wildfly.clustering.server.service.group.DistributedCacheGroupServiceConfiguratorProvider;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagerFactory;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.infinispan.session.metadata.SessionMetaDataKey;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.session.SessionManagerFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.SpecificationProvider;
import org.wildfly.common.function.Functions;

/**
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class InfinispanSessionManagerFactoryServiceConfigurator<S, SC, AL, LC> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, InfinispanSessionManagerFactoryConfiguration<S, SC, AL, LC>, Supplier<SessionManagerFactory<SC, LC, TransactionBatch>>, Consumer<ConfigurationBuilder> {

    private final InfinispanSessionManagementConfiguration<DeploymentUnit> configuration;
    private final SessionManagerFactoryConfiguration<S, SC, AL, LC> factoryConfiguration;

    private volatile ServiceConfigurator configurationConfigurator;
    private volatile ServiceConfigurator cacheConfigurator;
    private volatile ServiceConfigurator groupConfigurator;

    private volatile SupplierDependency<NodeFactory<Address>> group;
    private volatile SupplierDependency<KeyAffinityServiceFactory> affinityFactory;
    private volatile SupplierDependency<CommandDispatcherFactory> dispatcherFactory;
    @SuppressWarnings("rawtypes")
    private volatile Supplier<Cache> cache;

    public InfinispanSessionManagerFactoryServiceConfigurator(InfinispanSessionManagementConfiguration<DeploymentUnit> configuration, SessionManagerFactoryConfiguration<S, SC, AL, LC> factoryConfiguration) {
        super(ServiceName.JBOSS.append("clustering", "web", factoryConfiguration.getDeploymentName()));
        this.configuration = configuration;
        this.factoryConfiguration = factoryConfiguration;
    }

    @Override
    public SessionManagerFactory<SC, LC, TransactionBatch> get() {
        return new InfinispanSessionManagerFactory<>(this);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        String containerName = this.configuration.getContainerName();
        String cacheName = this.configuration.getCacheName();
        String deploymentName = this.factoryConfiguration.getDeploymentName();
        this.configurationConfigurator = new TemplateConfigurationServiceConfigurator(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, containerName, deploymentName), containerName, deploymentName, cacheName, this).configure(support);
        this.cacheConfigurator = new CacheServiceConfigurator<>(InfinispanCacheRequirement.CACHE.getServiceName(support, containerName, deploymentName), containerName, deploymentName).configure(support);
        this.groupConfigurator = new ProvidedCacheServiceConfigurator<>(DistributedCacheGroupServiceConfiguratorProvider.class, this.configuration.getContainerName(), this.factoryConfiguration.getDeploymentName()).configure(support);

        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, containerName));
        this.dispatcherFactory = new ServiceSupplierDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, containerName));
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, containerName, deploymentName));
        return this;
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        // Ensure expiration is not enabled on cache
        ExpirationConfiguration expiration = builder.expiration().create();
        if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
            builder.expiration().lifespan(-1).maxIdle(-1);
            InfinispanWebLogger.ROOT_LOGGER.expirationDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(this.configuration.getContainerName(), this.configuration.getCacheName()));
        }

        Integer size = this.factoryConfiguration.getMaxActiveSessions();
        EvictionStrategy strategy = (size != null) ? EvictionStrategy.REMOVE : EvictionStrategy.NONE;
        builder.memory().storage(StorageType.HEAP)
                .whenFull(strategy)
                .maxCount((size != null) ? size.longValue() : 0)
                ;
        if (strategy.isEnabled()) {
            // Only evict creation meta-data entries
            // We will cascade eviction to the remaining entries for a given session
            builder.addModule(DataContainerConfigurationBuilder.class).evictable(SessionMetaDataKey.class::isInstance);
        }
        PersistenceConfiguration persistence = builder.persistence().create();
        // If cache is configured to passivate and purge on startup, but application does not define a passivation threshold, then remove useless stores
        if ((size == null) && persistence.passivation() && persistence.stores().stream().allMatch(StoreConfiguration::purgeOnStartup)) {
            builder.persistence().passivation(false).clearStores();
        }
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> configurationBuilder = this.configurationConfigurator.build(target);
        ServiceBuilder<?> cacheBuilder = this.cacheConfigurator.build(target);
        ServiceBuilder<?> groupBuilder = this.groupConfigurator.build(target);

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionManagerFactory<SC, LC, TransactionBatch>> factory = new CompositeDependency(this.group, this.affinityFactory, this.dispatcherFactory).register(builder).provides(this.getServiceName());
        this.cache = builder.requires(this.cacheConfigurator.getServiceName());
        Service service = new FunctionalService<>(factory, Function.identity(), this, Functions.closingConsumer());
        return new CompositeServiceBuilder<>(List.of(configurationBuilder, cacheBuilder, groupBuilder, builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND)));
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.cache.get();
    }

    @Override
    public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
        return this.affinityFactory.get();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.get();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.group.get();
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.factoryConfiguration.getAttributePersistenceStrategy();
    }

    @Override
    public ByteBufferMarshaller getMarshaller() {
        return this.factoryConfiguration.getMarshaller();
    }

    @Override
    public Integer getMaxActiveSessions() {
        return this.factoryConfiguration.getMaxActiveSessions();
    }

    @Override
    public String getServerName() {
        return this.factoryConfiguration.getServerName();
    }

    @Override
    public String getDeploymentName() {
        return this.factoryConfiguration.getDeploymentName();
    }

    @Override
    public Supplier<LC> getLocalContextFactory() {
        return this.factoryConfiguration.getLocalContextFactory();
    }

    @Override
    public Immutability getImmutability() {
        return this.factoryConfiguration.getImmutability();
    }

    @Override
    public SpecificationProvider<S, SC, AL> getSpecificationProvider() {
        return this.factoryConfiguration.getSpecificationProvider();
    }
}
