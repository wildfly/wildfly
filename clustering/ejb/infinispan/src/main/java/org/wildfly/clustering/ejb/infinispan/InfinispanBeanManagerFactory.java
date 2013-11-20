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

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.marshalling.MarshalledValueFactory;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.as.clustering.marshalling.SimpleMarshalledValueFactory;
import org.jboss.as.clustering.marshalling.SimpleMarshallingContextFactory;
import org.jboss.as.clustering.marshalling.VersionedMarshallingConfiguration;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ejb.BeanContext;
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
@SuppressWarnings("rawtypes")
public class InfinispanBeanManagerFactory<G, I, T> extends AbstractService<BeanManagerFactory<G, I, T>> implements BeanManagerFactory<G, I, T> {

    private final BeanContext context;
    private final CacheInvoker invoker = new RetryingCacheInvoker(10, 100);
    private final Value<Cache> cache;
    private final Value<KeyAffinityServiceFactory> affinityFactory;
    private final Value<VersionedMarshallingConfiguration> config;
    private final BeanPassivationConfiguration passivationConfig;
    private final InjectedValue<NodeFactory> nodeFactory = new InjectedValue<>();
    private final InjectedValue<Registry> registry = new InjectedValue<>();

    public InfinispanBeanManagerFactory(BeanContext context, Value<VersionedMarshallingConfiguration> config, Value<Cache> cache, Value<KeyAffinityServiceFactory> affinityFactory, BeanPassivationConfiguration passivationConfig) {
        this.context = context;
        this.config = config;
        this.cache = cache;
        this.affinityFactory = affinityFactory;
        this.passivationConfig = passivationConfig;
    }

    @Override
    public BeanManager<G, I, T> createBeanManager(final IdentifierFactory<G> groupIdentifierFactory, final IdentifierFactory<I> beanIdentifierFactory, final PassivationListener<T> passivationListener, final RemoveListener<T> removeListener) {
        MarshallingContext context = new SimpleMarshallingContextFactory().createMarshallingContext(this.config.getValue(), this.context.getClassLoader());
        MarshalledValueFactory<MarshallingContext> factory = new SimpleMarshalledValueFactory(context);
        Cache<G, BeanGroupEntry<I, T>> groupCache = this.cache.getValue();
        org.infinispan.configuration.cache.Configuration config = groupCache.getCacheConfiguration();
        BeanGroupFactory<G, I, T> groupFactory = new InfinispanBeanGroupFactory<>(groupCache, this.invoker, factory, context);
        Configuration<G, G, BeanGroupEntry<I, T>, BeanGroupFactory<G, I, T>> groupConfiguration = new SimpleConfiguration<>(groupCache, groupFactory, groupIdentifierFactory);
        Cache<BeanKey<I>, BeanEntry<G>> beanCache = this.cache.getValue();
        String beanName = this.context.getBeanClass().getName();
        // If cache is clustered or configured with a write-through cache store
        // then we need to trigger any @PrePassivate/@PostActivate per request
        // See EJB.4.2.1 Instance Passivation and Conversational State
        final boolean evictionAllowed = config.persistence().usingStores();
        final boolean passivationEnabled = evictionAllowed && config.persistence().passivation();
        final boolean persistent = config.clustering().cacheMode().isClustered() || (evictionAllowed && !passivationEnabled);
        BeanFactory<G, I, T> beanFactory = new InfinispanBeanFactory<>(beanName, groupFactory, beanCache, this.invoker, this.context.getTimeout(), persistent ? passivationListener : null);
        Configuration<I, BeanKey<I>, BeanEntry<G>, BeanFactory<G, I, T>> beanConfiguration = new SimpleConfiguration<>(beanCache, beanFactory, beanIdentifierFactory);
        NodeFactory<Address> nodeFactory = this.nodeFactory.getValue();
        Registry<String, ?> registry = this.registry.getValue();
        final Time timeout = this.context.getTimeout();
        ExpirationConfiguration<T> expiration = new ExpirationConfiguration<T>() {
            @Override
            public Time getTimeout() {
                return timeout;
            }

            @Override
            public RemoveListener<T> getRemoveListener() {
                return removeListener;
            }
        };
        final BeanPassivationConfiguration passivationConfig = this.passivationConfig;
        PassivationConfiguration<T> passivation = new PassivationConfiguration<T>() {
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
        };
        return new InfinispanBeanManager<>(beanName, beanConfiguration, groupConfiguration, this.affinityFactory.getValue(), registry, nodeFactory, expiration, passivation);
    }

    @Override
    public BeanManagerFactory<G, I, T> getValue() {
        return this;
    }

    Injector<NodeFactory> getNodeFactoryInjector() {
        return this.nodeFactory;
    }

    Injector<Registry> getRegistryInjector() {
        return this.registry;
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
