/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.hotrod.bean;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.bean.BeanManagerConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanAccessMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaData;
import org.wildfly.clustering.ejb.cache.bean.BeanFactory;
import org.wildfly.clustering.ejb.cache.bean.BeanMetaDataFactory;
import org.wildfly.clustering.ejb.cache.bean.CompositeBeanFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.client.listener.ListenerRegistrar;

/**
 * A factory that creates a HotRod-based {@link BeanManager} implementation.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class HotRodBeanManagerFactory<K, V extends BeanInstance<K>> implements BeanManagerFactory<K, V, TransactionBatch> {

    private final HotRodBeanManagerFactoryConfiguration<K, V> configuration;

    public HotRodBeanManagerFactory(HotRodBeanManagerFactoryConfiguration<K, V> configuration) {
        this.configuration = configuration;
    }

    @Override
    public BeanManager<K, V, TransactionBatch> createBeanManager(BeanManagerConfiguration<K, V> configuration) {
        HotRodBeanManagerConfiguration<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> managerConfiguration = new DefaultHotRodBeanManagerConfiguration<>(this.configuration, configuration);
        ListenerRegistrar registrar = new HotRodBeanExpirationListener<>(managerConfiguration);
        return new HotRodBeanManager<>(managerConfiguration, registrar);
    }

    private static class DefaultHotRodBeanManagerConfiguration<K, V extends BeanInstance<K>> implements HotRodBeanManagerConfiguration<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> {
        private final HotRodBeanManagerFactoryConfiguration<K, V> factoryConfiguration;
        private final BeanManagerConfiguration<K, V> managerConfiguration;
        private final BeanFactory<K, V, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> beanFactory;

        DefaultHotRodBeanManagerConfiguration(HotRodBeanManagerFactoryConfiguration<K, V> factoryConfiguration, BeanManagerConfiguration<K, V> managerConfiguration) {
            this.factoryConfiguration = factoryConfiguration;
            this.managerConfiguration = managerConfiguration;
            BeanMetaDataFactory<K, Map.Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> metaDataFactory = new HotRodBeanMetaDataFactory<>(this);
            this.beanFactory =  new CompositeBeanFactory<>(metaDataFactory, this.factoryConfiguration.getBeanGroupManager());
        }

        @Override
        public BeanFactory<K, V, Entry<BeanCreationMetaData<K>, BeanAccessMetaData>> getBeanFactory() {
            return this.beanFactory;
        }

        @Override
        public Group getGroup() {
            return this.factoryConfiguration.getGroup();
        }

        @Override
        public Supplier<K> getIdentifierFactory() {
            return this.managerConfiguration.getIdentifierFactory();
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
        public <CK, CV> RemoteCache<CK, CV> getCache() {
            return this.factoryConfiguration.getCache();
        }
    }
}
