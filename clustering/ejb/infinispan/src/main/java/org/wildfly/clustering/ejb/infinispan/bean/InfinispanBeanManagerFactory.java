/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.Map;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.bean.BeanManagerConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanAccessMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
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

    private static class DefaultInfinispanBeanManagerConfiguration<K, V extends BeanInstance<K>> implements InfinispanBeanManagerConfiguration<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> {
        private final InfinispanBeanManagerFactoryConfiguration<K, V> factoryConfiguration;
        private final BeanManagerConfiguration<K, V> managerConfiguration;
        private final BeanFactory<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> beanFactory;

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
        public BeanFactory<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> getBeanFactory() {
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
