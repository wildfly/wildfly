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
package org.wildfly.clustering.ejb.infinispan;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheProperties;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanPassivationConfiguration;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.IdentifierFactory;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanFactory;
import org.wildfly.clustering.ejb.infinispan.group.InfinispanBeanGroupFactory;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;

/**
 * Factory for creating an infinispan-based {@link BeanManager}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBeanManagerFactory<I, T> implements BeanManagerFactory<I, T, TransactionBatch> {

    private final InfinispanBeanManagerFactoryConfiguration configuration;

    public InfinispanBeanManagerFactory(InfinispanBeanManagerFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public BeanManager<I, T, TransactionBatch> createBeanManager(IdentifierFactory<I> identifierFactory, PassivationListener<T> passivationListener, RemoveListener<T> removeListener) {
        MarshallingContext context = new SimpleMarshallingContextFactory().createMarshallingContext(this.configuration.getMarshallingConfigurationRepository(), this.configuration.getBeanContext().getClassLoader());
        MarshalledValueFactory<MarshallingContext> factory = new SimpleMarshalledValueFactory(context);
        Cache<BeanKey<I>, BeanEntry<I>> beanCache = this.configuration.getCache();
        Cache<BeanGroupKey<I>, BeanGroupEntry<I, T>> groupCache = this.configuration.getCache();
        CacheProperties properties = new InfinispanCacheProperties(groupCache.getCacheConfiguration());
        String beanName = this.configuration.getBeanContext().getBeanName();
        BeanPassivationConfiguration passivationConfig = this.configuration.getPassivationConfiguration();
        PassivationConfiguration<T> passivation = new PassivationConfiguration<T>() {
            @Override
            public PassivationListener<T> getPassivationListener() {
                return passivationListener;
            }

            @Override
            public BeanPassivationConfiguration getConfiguration() {
                return passivationConfig;
            }
        };
        Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> beanFilter = new BeanFilter<>(beanName);
        BeanGroupFactory<I, T> groupFactory = new InfinispanBeanGroupFactory<>(groupCache, beanCache, beanFilter, factory, context, properties, passivation);
        Configuration<BeanGroupKey<I>, BeanGroupEntry<I, T>, BeanGroupFactory<I, T>> groupConfiguration = new SimpleConfiguration<>(groupCache, groupFactory);
        BeanFactory<I, T> beanFactory = new InfinispanBeanFactory<>(beanName, groupFactory, beanCache, properties, this.configuration.getBeanContext().getTimeout(), properties.isPersistent() ? passivationListener : null);
        Configuration<BeanKey<I>, BeanEntry<I>, BeanFactory<I, T>> beanConfiguration = new SimpleConfiguration<>(beanCache, beanFactory);
        NodeFactory<Address> nodeFactory = this.configuration.getNodeFactory();
        Registry<String, ?> registry = this.configuration.getRegistry();
        KeyAffinityServiceFactory affinityFactory = this.configuration.getKeyAffinityServiceFactory();
        CommandDispatcherFactory dispatcherFactory = this.configuration.getCommandDispatcherFactory();
        Time timeout = this.configuration.getBeanContext().getTimeout();
        ScheduledExecutorService scheduler = this.configuration.getScheduler();
        ExpirationConfiguration<T> expiration = new ExpirationConfiguration<T>() {
            @Override
            public Time getTimeout() {
                return timeout;
            }

            @Override
            public RemoveListener<T> getRemoveListener() {
                return removeListener;
            }

            @Override
            public ScheduledExecutorService getExecutor() {
                return scheduler;
            }
        };
        InfinispanBeanManagerConfiguration<I, T> configuration = new InfinispanBeanManagerConfiguration<I, T>() {
            @Override
            public Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> getBeanFilter() {
                return beanFilter;
            }

            @Override
            public KeyAffinityServiceFactory getAffinityFactory() {
                return affinityFactory;
            }

            @Override
            public Registry<String, ?> getRegistry() {
                return registry;
            }

            @Override
            public NodeFactory<Address> getNodeFactory() {
                return nodeFactory;
            }

            @Override
            public CommandDispatcherFactory getCommandDispatcherFactory() {
                return dispatcherFactory;
            }

            @Override
            public ExpirationConfiguration<T> getExpirationConfiguration() {
                return expiration;
            }

            @Override
            public PassivationConfiguration<T> getPassivationConfiguration() {
                return passivation;
            }

            @Override
            public CacheProperties getProperties() {
                return properties;
            }
        };
        return new InfinispanBeanManager<>(configuration, identifierFactory, beanConfiguration, groupConfiguration);
    }

    private static class SimpleConfiguration<K, V, F> implements Configuration<K, V, F> {
        private final F factory;
        private final Cache<K, V> cache;

        SimpleConfiguration(Cache<K, V> cache, F factory) {
            this.factory = factory;
            this.cache = cache;
        }

        @Override
        public F getFactory() {
            return this.factory;
        }

        @Override
        public Cache<K, V> getCache() {
            return this.cache;
        }
    }
}
