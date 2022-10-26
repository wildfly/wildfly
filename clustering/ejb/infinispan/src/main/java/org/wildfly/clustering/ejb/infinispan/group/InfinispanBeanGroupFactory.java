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
package org.wildfly.clustering.ejb.infinispan.group;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.util.concurrent.BlockingManager;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanMutatorFactory;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.infinispan.BeanEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroupFactory;
import org.wildfly.clustering.ejb.infinispan.BeanGroupFilter;
import org.wildfly.clustering.ejb.infinispan.BeanGroupKey;
import org.wildfly.clustering.ejb.infinispan.BeanKey;
import org.wildfly.clustering.ejb.infinispan.PassivationConfiguration;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.listener.ListenerRegistration;
import org.wildfly.clustering.infinispan.listener.PostActivateBlockingListener;
import org.wildfly.clustering.infinispan.listener.PrePassivateBlockingListener;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;

/**
 * Encapsulates the cache mapping strategy of a bean group.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 * @param <C> the marshalling context
 */
public class InfinispanBeanGroupFactory<I, T, C> implements BeanGroupFactory<I, T, C> {

    private final Cache<BeanGroupKey<I>, BeanGroupEntry<I, T, C>> cache;
    private final Cache<BeanGroupKey<I>, BeanGroupEntry<I, T, C>> findCache;
    private final Cache<BeanKey<I>, BeanEntry<I>> beanCache;
    private final Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> beanFilter;
    private final MarshalledValueFactory<C> factory;
    private final PassivationListener<T> passivationListener;
    private final MutatorFactory<BeanGroupKey<I>, BeanGroupEntry<I, T, C>> mutatorFactory;
    private final ListenerRegistration prePassivateListenerRegistration;
    private final ListenerRegistration postActivateListenerRegistration;
    private final Executor executor;

    public InfinispanBeanGroupFactory(Cache<BeanGroupKey<I>, BeanGroupEntry<I, T, C>> cache, Cache<BeanKey<I>, BeanEntry<I>> beanCache, Predicate<Map.Entry<? super BeanKey<I>, ? super BeanEntry<I>>> beanFilter, MarshalledValueFactory<C> factory, CacheProperties properties, PassivationConfiguration<T> passivation) {
        this.cache = cache;
        this.findCache = properties.isLockOnRead() ? cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : cache;
        this.beanCache = beanCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_LISTENER_NOTIFICATION);
        this.beanFilter = beanFilter;
        this.factory = factory;
        this.passivationListener = !properties.isPersistent() ? passivation.getPassivationListener() : null;
        @SuppressWarnings("deprecation")
        BlockingManager blocking = cache.getCacheManager().getGlobalComponentRegistry().getComponent(BlockingManager.class);
        this.executor = blocking.asExecutor(this.getClass().getName());
        this.prePassivateListenerRegistration = new PrePassivateBlockingListener<>(this.cache, this::prePassivate).register(BeanGroupFilter.INSTANCE);
        this.postActivateListenerRegistration = new PostActivateBlockingListener<>(this.cache, this::postActivate).register(BeanGroupFilter.INSTANCE);
        this.mutatorFactory = new InfinispanMutatorFactory<>(cache, properties);
    }

    @Override
    public void close() {
        this.prePassivateListenerRegistration.close();
        this.postActivateListenerRegistration.close();
    }

    @Override
    public int getPassiveCount() {
        return this.cache.getCacheConfiguration().persistence().passivation() ? this.count(EnumSet.noneOf(Flag.class)) - this.count(EnumSet.of(Flag.SKIP_CACHE_LOAD)) : 0;
    }

    private int count(Set<Flag> flags) {
        Cache<BeanKey<I>, BeanEntry<I>> cache = flags.isEmpty() ? this.beanCache : this.beanCache.getAdvancedCache().withFlags(flags);
        try (Stream<?> keys = cache.keySet().stream()) {
            return (int) keys.filter(InfinispanBeanKey.class::isInstance).count();
        }
    }

    @Override
    public BeanGroupKey<I> createKey(I id) {
        return new InfinispanBeanGroupKey<>(id);
    }

    @Override
    public BeanGroupEntry<I, T, C> createValue(I id, Void context) {
        BeanGroupEntry<I, T, C> entry = new InfinispanBeanGroupEntry<>(this.factory.createMarshalledValue(new ConcurrentHashMap<>()));
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(this.createKey(id), entry);
        return entry;
    }

    @Override
    public BeanGroupEntry<I, T, C> findValue(I id) {
        return this.findCache.get(this.createKey(id));
    }

    @Override
    public BeanGroupEntry<I, T, C> tryValue(I id) {
        return this.findCache.getAdvancedCache().withFlags(Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY).get(this.createKey(id));
    }

    @Override
    public boolean remove(I id) {
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(this.createKey(id));
        return true;
    }

    @Override
    public BeanGroup<I, T> createGroup(I id, BeanGroupEntry<I, T, C> entry) {
        return this.createGroup(id, entry, this.mutatorFactory.createMutator(this.createKey(id), entry));
    }

    private BeanGroup<I, T> createGroup(I id, BeanGroupEntry<I, T, C> entry, Mutator mutator) {
        return new InfinispanBeanGroup<>(id, entry, this.factory.getMarshallingContext(), mutator, this);
    }

    @Override
    public void evict(I id) {
        this.cache.evict(new InfinispanBeanGroupKey<>(id));
    }

    void prePassivate(BeanGroupKey<I> key, BeanGroupEntry<I, T, C> entry) {
        try (BeanGroup<I, T> group = new InfinispanBeanGroup<>(key.getId(), entry, this.factory.getMarshallingContext(), Mutator.PASSIVE, this)) {
            Set<I> beans = group.getBeans();
            List<I> notified = new ArrayList<>(beans.size());
            try {
                for (I beanId : beans) {
                    BeanKey<I> beanKey = new InfinispanBeanKey<>(beanId);
                    BeanEntry<I> beanEntry = this.beanCache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD).get(beanKey);
                    if ((beanEntry != null) && this.beanFilter.test(new AbstractMap.SimpleImmutableEntry<>(beanKey, beanEntry))) {
                        InfinispanEjbLogger.ROOT_LOGGER.tracef("Passivating bean %s", beanKey);
                        group.prePassivate(beanId, this.passivationListener);
                        notified.add(beanId);
                        // Cascade evict to bean entry
                        this.executor.execute(() -> this.beanCache.evict(beanKey));
                    }
                }
            } catch (RuntimeException | Error e) {
                // Restore state of pre-passivated beans
                for (I beanId : notified) {
                    try {
                        group.postActivate(beanId, this.passivationListener);
                    } catch (RuntimeException | Error ex) {
                        InfinispanEjbLogger.ROOT_LOGGER.warn(ex.getLocalizedMessage(), ex);
                    }
                }
                // Abort passivation if any beans failed to pre-passivate
                throw e;
            }
        }
    }

    void postActivate(BeanGroupKey<I> key, BeanGroupEntry<I, T, C> entry) {
        C context = this.factory.getMarshallingContext();
        try (BeanGroup<I, T> group = new InfinispanBeanGroup<>(key.getId(), entry, context, Mutator.PASSIVE, this)) {
            for (I beanId : group.getBeans()) {
                BeanKey<I> beanKey = new InfinispanBeanKey<>(beanId);
                BeanEntry<I> beanEntry = this.beanCache.get(beanKey);
                if ((beanEntry != null) && this.beanFilter.test(new AbstractMap.SimpleImmutableEntry<>(beanKey, beanEntry))) {
                    InfinispanEjbLogger.ROOT_LOGGER.tracef("Activating bean %s", beanKey);
                    try {
                        group.postActivate(beanId, this.passivationListener);
                    } catch (RuntimeException | Error e) {
                        InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                }
            }
        }
    }
}
