/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.bean.BeanManagerConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.RemappableBeanMetaDataEntry;
import org.wildfly.clustering.ejb.cache.bean.CompositeBeanFactory;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;

/**
 * Factory for creating an infinispan-based {@link BeanManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class InfinispanBeanManagerFactory<K, V extends BeanInstance<K>> implements BeanManagerFactory<K, V, TransactionBatch> {

    private final InfinispanBeanManagerFactoryConfiguration<K, V> configuration;

    public InfinispanBeanManagerFactory(InfinispanBeanManagerFactoryConfiguration<K, V> configuration) {
        this.configuration = configuration;
    }

    @Override
    public BeanManager<K, V, TransactionBatch> createBeanManager(BeanManagerConfiguration<K, V> configuration) {
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
        public BeanExpirationConfiguration<K, V> getExpiration() {
            return this.managerConfiguration.getExpiration();
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
        public KeyAffinityServiceFactory getAffinityFactory() {
            return this.factoryConfiguration.getKeyAffinityServiceFactory();
        }

        @Override
        public Group<Address> getGroup() {
            return this.factoryConfiguration.getGroup();
        }

        @Override
        public CommandDispatcherFactory getCommandDispatcherFactory() {
            return this.factoryConfiguration.getCommandDispatcherFactory();
        }
    }
}
