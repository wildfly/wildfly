/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.CompositeServiceBuilder;
import org.jboss.as.clustering.controller.CompositeServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.BeanConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactory;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.infinispan.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.server.service.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerManagerFactoryServiceConfigurator<I, C> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, InfinispanTimerManagerFactoryConfiguration<I>, Supplier<TimerManagerFactory<I, TransactionBatch>>, Consumer<ConfigurationBuilder> {

    private final InfinispanTimerManagementConfiguration configuration;
    private final TimerManagerFactoryConfiguration<I> factoryConfiguration;
    private final boolean persistent;

    private volatile CompositeServiceConfigurator dependenciesConfigurator;
    private volatile SupplierDependency<KeyAffinityServiceFactory> affinityFactory;
    private volatile SupplierDependency<CommandDispatcherFactory> dispatcherFactory;
    private volatile SupplierDependency<Group<Address>> group;
    @SuppressWarnings("rawtypes")
    private volatile Supplier<Cache> cache;

    public InfinispanTimerManagerFactoryServiceConfigurator(InfinispanTimerManagementConfiguration configuration, TimerManagerFactoryConfiguration<I> factoryConfiguration) {
        super(ServiceName.JBOSS.append("clustering", "timer").append(factoryConfiguration.getBeanConfiguration().getName()));
        this.configuration = configuration;
        this.factoryConfiguration = factoryConfiguration;
        this.persistent = factoryConfiguration.isPersistent();
    }

    @Override
    public TimerManagerFactory<I, TransactionBatch> get() {
        return new InfinispanTimerManagerFactory<>(this);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        String containerName = this.configuration.getContainerName();
        String cacheName = this.configuration.getCacheName();
        String beanName = this.factoryConfiguration.getBeanConfiguration().getName();

        ServiceName cacheServiceName = InfinispanCacheRequirement.CACHE.getServiceName(support, containerName, beanName);
        this.dependenciesConfigurator = new CompositeServiceConfigurator(cacheServiceName);
        this.dependenciesConfigurator.accept(new TemplateConfigurationServiceConfigurator(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, containerName, beanName), containerName, beanName, cacheName, this).configure(support));
        this.dependenciesConfigurator.accept(new CacheServiceConfigurator<>(cacheServiceName, containerName, beanName).configure(support));

        for (CacheServiceConfiguratorProvider provider : ServiceLoader.load(DistributedCacheServiceConfiguratorProvider.class, DistributedCacheServiceConfiguratorProvider.class.getClassLoader())) {
            for (ServiceConfigurator configurator : provider.getServiceConfigurators(support, this.configuration.getContainerName(), beanName)) {
                this.dependenciesConfigurator.accept(configurator);
            }
        }

        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, containerName));
        this.dispatcherFactory = new ServiceSupplierDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, containerName));
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, containerName, beanName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<TimerManagerFactory<I, TransactionBatch>> consumer = new CompositeDependency(this.affinityFactory, this.dispatcherFactory, this.group).register(builder).provides(name);
        this.cache = builder.requires(this.dependenciesConfigurator.getServiceName());
        builder.setInstance(new FunctionalService<>(consumer, Function.identity(), this)).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeServiceBuilder<>(List.of(this.dependenciesConfigurator.build(target), builder));
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        // Ensure expiration is not enabled on cache
        ExpirationConfiguration expiration = builder.expiration().create();
        if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
            builder.expiration().lifespan(-1).maxIdle(-1);
        }

        Integer size = this.configuration.getMaxActiveTimers();
        EvictionStrategy strategy = (size != null) ? EvictionStrategy.REMOVE : EvictionStrategy.NONE;
        builder.memory().storage(StorageType.HEAP).whenFull(strategy).maxCount((size != null) ? size.longValue() : 0);
        if (strategy.isEnabled()) {
            // Only evict creation meta-data entries
            // We will cascade eviction to the remaining entries for a given session
            builder.addModule(DataContainerConfigurationBuilder.class).evictable(TimerCreationMetaDataKey.class::isInstance);
        }

        builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).transactionManagerLookup(() -> EmbeddedTransactionManager.getInstance()).lockingMode(LockingMode.PESSIMISTIC).locking().isolationLevel(IsolationLevel.REPEATABLE_READ);
        builder.persistence().passivation(false);
    }

    @Override
    public BeanConfiguration getBeanConfiguration() {
        return this.factoryConfiguration.getBeanConfiguration();
    }

    @Override
    public Supplier<I> getIdentifierFactory() {
        return this.factoryConfiguration.getIdentifierFactory();
    }

    @Override
    public ByteBufferMarshaller getMarshaller() {
        return this.configuration.getMarshallerFactory().apply(this.factoryConfiguration.getBeanConfiguration().getModule());
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
    public Group<Address> getGroup() {
        return this.group.get();
    }

    @Override
    public TimerRegistry<I> getRegistry() {
        return this.factoryConfiguration.getRegistry();
    }

    @Override
    public boolean isPersistent() {
        return this.persistent;
    }
}
