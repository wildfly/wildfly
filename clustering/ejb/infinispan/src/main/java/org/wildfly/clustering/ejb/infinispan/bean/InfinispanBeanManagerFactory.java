/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import java.time.Duration;
import java.util.Optional;

import org.infinispan.Cache;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.bean.BeanManagerConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.CompositeBeanFactory;
import org.wildfly.clustering.ejb.cache.bean.RemappableBeanMetaDataEntry;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;

/**
 * Factory for creating an infinispan-based {@link BeanManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class InfinispanBeanManagerFactory<K, V extends BeanInstance<K>> implements BeanManagerFactory<K, V> {

    private final InfinispanBeanManagerFactoryConfiguration<K, V> configuration;

    public InfinispanBeanManagerFactory(InfinispanBeanManagerFactoryConfiguration<K, V> configuration) {
        this.configuration = configuration;
    }

    @Override
    public BeanManager<K, V> createBeanManager(BeanManagerConfiguration<K, V> configuration) {
        return new InfinispanBeanManager<>(new DefaultInfinispanBeanManagerConfiguration<>(this.configuration, configuration));
    }

    private static class DefaultInfinispanBeanManagerConfiguration<K, V extends BeanInstance<K>> implements InfinispanBeanManagerConfiguration<K, V, RemappableBeanMetaDataEntry<K>> {
        private final InfinispanBeanManagerFactoryConfiguration<K, V> factoryConfiguration;
        private final BeanManagerConfiguration<K, V> managerConfiguration;
        private final BeanFactory<K, V, RemappableBeanMetaDataEntry<K>> beanFactory;

        DefaultInfinispanBeanManagerConfiguration(InfinispanBeanManagerFactoryConfiguration<K, V> factoryConfiguration, BeanManagerConfiguration<K, V> managerConfiguration) {
            this.factoryConfiguration = factoryConfiguration;
            this.managerConfiguration = managerConfiguration;
            this.beanFactory = new CompositeBeanFactory<>(new InfinispanBeanMetaDataFactory<>(this), factoryConfiguration.getBeanGroupManager());
        }

        @Override
        public Consumer<V> getExpirationListener() {
            return this.managerConfiguration.getExpirationListener();
        }

        @Override
        public Optional<Duration> getMaxIdle() {
            return this.managerConfiguration.getMaxIdle();
        }

        @Override
        public String getBeanName() {
            return this.managerConfiguration.getBeanName();
        }

        @Override
        public <KK, VV> Cache<KK, VV> getCache() {
            return this.factoryConfiguration.getCache();
        }

        @Override
        public BeanFactory<K, V, RemappableBeanMetaDataEntry<K>> getBeanFactory() {
            return this.beanFactory;
        }

        @Override
        public Supplier<K> getIdentifierFactory() {
            return this.managerConfiguration.getIdentifierFactory();
        }

        @Override
        public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
            return this.factoryConfiguration.getCommandDispatcherFactory();
        }
    }
}
