/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.configuration.cache.IsolationLevel;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.tm.EmbeddedTransactionManager;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.cache.infinispan.embedded.container.DataContainerConfigurationBuilder;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey;
import org.wildfly.clustering.ejb.timer.TimerManagementConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.ejb.timer.TimerRegistry;
import org.wildfly.clustering.ejb.timer.TimerServiceConfiguration;
import org.wildfly.clustering.infinispan.service.CacheConfigurationServiceInstaller;
import org.wildfly.clustering.infinispan.service.CacheServiceInstaller;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerManagementProvider implements TimerManagementProvider, UnaryOperator<ConfigurationBuilder> {

    private final TimerManagementConfiguration configuration;
    private final BinaryServiceConfiguration cacheConfiguration;

    public InfinispanTimerManagementProvider(TimerManagementConfiguration configuration, BinaryServiceConfiguration cacheConfiguration) {
        this.configuration = configuration;
        this.cacheConfiguration = cacheConfiguration;
    }

    @Override
    public <I> Iterable<ServiceInstaller> getTimerManagerFactoryServiceInstallers(ServiceName name, TimerManagerFactoryConfiguration<I> configuration) {
        BinaryServiceConfiguration timerManagerCacheConfiguration = this.cacheConfiguration.withChildName(configuration.getTimerServiceConfiguration().getName());

        ServiceInstaller cacheConfigurationInstaller = new CacheConfigurationServiceInstaller(timerManagerCacheConfiguration, CacheConfigurationServiceInstaller.fromTemplate(this.cacheConfiguration).map(this));
        ServiceInstaller cacheInstaller = new CacheServiceInstaller(timerManagerCacheConfiguration);

        ServiceDependency<CacheContainerCommandDispatcherFactory> commandDispatcherFactory = timerManagerCacheConfiguration.getServiceDependency(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).map(CacheContainerCommandDispatcherFactory.class::cast);
        ServiceDependency<Cache<?, ?>> cache = timerManagerCacheConfiguration.getServiceDependency(InfinispanServiceDescriptor.CACHE);
        InfinispanTimerManagerFactoryConfiguration<I> factoryConfiguration = new InfinispanTimerManagerFactoryConfiguration<>() {
            @Override
            public TimerServiceConfiguration getTimerServiceConfiguration() {
                return configuration.getTimerServiceConfiguration();
            }

            @Override
            public TimerRegistry<I> getRegistry() {
                return configuration.getRegistry();
            }

            @Override
            public boolean isPersistent() {
                return configuration.isPersistent();
            }

            @Override
            public Supplier<I> getIdentifierFactory() {
                return configuration.getIdentifierFactory();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <K, V> Cache<K, V> getCache() {
                return (Cache<K, V>) cache.get();
            }

            @Override
            public ByteBufferMarshaller getMarshaller() {
                return InfinispanTimerManagementProvider.this.configuration.getMarshallerFactory().apply(configuration.getTimerServiceConfiguration().getModule());
            }

            @Override
            public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
                return commandDispatcherFactory.get();
            }
        };
        ServiceInstaller factoryInstaller = ServiceInstaller.builder(Functions.constantSupplier(new InfinispanTimerManagerFactory<>(factoryConfiguration)))
                .provides(name)
                .requires(List.of(commandDispatcherFactory, cache))
                .build();
        return List.of(cacheConfigurationInstaller, cacheInstaller, factoryInstaller);
    }

    @Override
    public ConfigurationBuilder apply(ConfigurationBuilder builder) {
        // Ensure expiration is not enabled on cache
        ExpirationConfiguration expiration = builder.expiration().create();
        if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
            builder.expiration().lifespan(-1).maxIdle(-1);
        }

        OptionalInt size = this.configuration.getMaxActiveTimers();
        EvictionStrategy strategy = size.isPresent() ? EvictionStrategy.REMOVE : EvictionStrategy.NONE;
        builder.memory().storage(StorageType.HEAP).whenFull(strategy).maxCount(size.orElse(0));
        if (strategy.isEnabled()) {
            // Only evict creation meta-data entries
            // We will cascade eviction to the remaining entries for a given session
            builder.addModule(DataContainerConfigurationBuilder.class).evictable(TimerMetaDataKey.class::isInstance);
        }

        builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).transactionManagerLookup(EmbeddedTransactionManager::getInstance).lockingMode(LockingMode.PESSIMISTIC).locking().isolationLevel(IsolationLevel.REPEATABLE_READ);
        return builder;
    }
}
