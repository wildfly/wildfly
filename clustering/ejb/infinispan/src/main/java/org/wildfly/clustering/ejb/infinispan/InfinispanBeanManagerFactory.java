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

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
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
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.MarshallingContext;
import org.wildfly.clustering.marshalling.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.SimpleMarshallingContextFactory;
import org.wildfly.clustering.registry.Registry;

/**
 * Factory for creating an infinispan-based {@link BeanManager}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBeanManagerFactory<G, I, T> implements BeanManagerFactory<G, I, T, TransactionBatch> {

    private final InfinispanBeanManagerFactoryConfiguration configuration;

    public InfinispanBeanManagerFactory(InfinispanBeanManagerFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public BeanManager<G, I, T, TransactionBatch> createBeanManager(final IdentifierFactory<G> groupIdentifierFactory, final IdentifierFactory<I> beanIdentifierFactory, final PassivationListener<T> passivationListener, final RemoveListener<T> removeListener) {
        MarshallingContext context = new SimpleMarshallingContextFactory().createMarshallingContext(this.configuration.getMarshallingConfiguration(), this.configuration.getBeanContext().getClassLoader());
        MarshalledValueFactory<MarshallingContext> factory = new SimpleMarshalledValueFactory(context);
        Cache<G, BeanGroupEntry<I, T>> groupCache = this.configuration.getCache();
        org.infinispan.configuration.cache.Configuration config = groupCache.getCacheConfiguration();
        BeanGroupFactory<G, I, T> groupFactory = new InfinispanBeanGroupFactory<>(groupCache, factory, context);
        Configuration<G, G, BeanGroupEntry<I, T>, BeanGroupFactory<G, I, T>> groupConfiguration = new SimpleConfiguration<>(groupCache, groupFactory, groupIdentifierFactory);
        Cache<BeanKey<I>, BeanEntry<G>> beanCache = this.configuration.getCache();
        final String beanName = this.configuration.getBeanContext().getBeanName();
        // If cache is clustered or configured with a write-through cache store
        // then we need to trigger any @PrePassivate/@PostActivate per request
        // See EJB.4.2.1 Instance Passivation and Conversational State
        final boolean evictionAllowed = config.persistence().usingStores();
        final boolean passivationEnabled = evictionAllowed && config.persistence().passivation();
        final boolean persistent = config.clustering().cacheMode().isClustered() || (evictionAllowed && !passivationEnabled);
        BeanFactory<G, I, T> beanFactory = new InfinispanBeanFactory<>(beanName, groupFactory, beanCache, this.configuration.getBeanContext().getTimeout(), persistent ? passivationListener : null);
        Configuration<I, BeanKey<I>, BeanEntry<G>, BeanFactory<G, I, T>> beanConfiguration = new SimpleConfiguration<>(beanCache, beanFactory, beanIdentifierFactory);
        final NodeFactory<Address> nodeFactory = this.configuration.getNodeFactory();
        final Registry<String, ?> registry = this.configuration.getRegistry();
        final KeyAffinityServiceFactory affinityFactory = this.configuration.getKeyAffinityServiceFactory();
        final CommandDispatcherFactory dispatcherFactory = this.configuration.getCommandDispatcherFactory();
        final Time timeout = this.configuration.getBeanContext().getTimeout();
        final ScheduledExecutorService scheduler = this.configuration.getScheduler();
        final ExpirationConfiguration<T> expiration = new ExpirationConfiguration<T>() {
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
        final Executor executor = this.configuration.getExecutor();
        final BeanPassivationConfiguration passivationConfig = this.configuration.getPassivationConfiguration();
        final PassivationConfiguration<T> passivation = new PassivationConfiguration<T>() {
            @Override
            public PassivationListener<T> getPassivationListener() {
                return passivationListener;
            }

            @Override
            public boolean isEvictionAllowed() {
                return evictionAllowed;
            }

            @Override
            public boolean isPersistent() {
                return persistent;
            }

            @Override
            public BeanPassivationConfiguration getConfiguration() {
                return passivationConfig;
            }

            @Override
            public Executor getExecutor() {
                return executor;
            }
        };
        InfinispanBeanManagerConfiguration<T> configuration = new InfinispanBeanManagerConfiguration<T>() {
            @Override
            public String getBeanName() {
                return beanName;
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
        };
        return new InfinispanBeanManager<>(configuration, beanConfiguration, groupConfiguration);
    }

    private static class SimpleConfiguration<I, K, V, F> implements Configuration<I, K, V, F> {
        private final F factory;
        private final Cache<K, V> cache;
        private final IdentifierFactory<I> identifierFactory;

        SimpleConfiguration(Cache<K, V> cache, F factory, IdentifierFactory<I> identifierFactory) {
            this.factory = factory;
            this.cache = cache;
            this.identifierFactory = identifierFactory;
        }

        @Override
        public F getFactory() {
            return this.factory;
        }

        @Override
        public Cache<K, V> getCache() {
            return this.cache;
        }

        @Override
        public IdentifierFactory<I> getIdentifierFactory() {
            return this.identifierFactory;
        }
    }
}
