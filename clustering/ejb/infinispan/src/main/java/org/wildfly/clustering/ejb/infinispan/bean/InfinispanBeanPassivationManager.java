/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.embedded.listener.ListenerRegistration;
import org.wildfly.clustering.cache.infinispan.embedded.listener.PassivationCacheEventListenerRegistrar;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.IntPredicate;
import org.wildfly.clustering.marshalling.MarshalledValue;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanPassivationManager<K, V extends BeanInstance<K>, C> implements BeanPassivationManager {
    private static final Function<String, AtomicInteger> COUNTER_FACTORY = Consumer.<String>of().thenReturn(AtomicInteger::new);
    private static final IntUnaryOperator INCREMENT = Math::incrementExact;
    private static final IntUnaryOperator DECREMENT = org.wildfly.clustering.function.IntUnaryOperator.when(IntPredicate.POSITIVE, Math::decrementExact, IntUnaryOperator.identity());

    private final Cache<Key<K>, ?> cache;
    private final C context;
    private final Executor executor;
    private final ListenerRegistration passivationListenerRegistration;
    // Track number of passivated bean instances per component
    // Filtering and counting cache store entries is prohibitively expensive
    private final Map<String, AtomicInteger> passivations = new ConcurrentHashMap<>();

    public InfinispanBeanPassivationManager(EmbeddedCacheConfiguration configuration, C context) {
        this.cache = configuration.getCache();
        this.context = context;
        // We only need to listen for activation/passivation events for non-persistent caches
        // pre-passivate/post-activate callbacks for persistent caches are triggered via GroupManager
        this.executor = !configuration.getCacheProperties().isPersistent() ? configuration.getExecutor() : null;
        Cache<BeanGroupKey<K>, MarshalledValue<Map<K, V>, C>> beanGroupCache = configuration.getCache();
        this.passivationListenerRegistration = (this.executor != null) ? new PassivationCacheEventListenerRegistrar<>(beanGroupCache, this::prePassivate, this::postActivate).register(BeanGroupKey.class) : ListenerRegistration.EMPTY;
    }

    @Override
    public int applyAsInt(String name) {
        AtomicInteger count = this.passivations.get(name);
        return (count != null) ? count.get() : 0;
    }

    @Override
    public void close() {
        this.passivationListenerRegistration.close();
    }

    void postActivate(BeanGroupKey<K> key, MarshalledValue<Map<K, V>, C> value) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Received post-activate event for bean group %s", key.getId());
        try {
            Map<K, V> instances = value.get(this.context);
            for (V instance : instances.values()) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Invoking post-activate callback for bean %s", instance.getId());
                instance.postActivate();
                this.passivations.computeIfAbsent(instance.getName(), COUNTER_FACTORY).updateAndGet(DECREMENT);
            }
        } catch (IOException e) {
            InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    void prePassivate(BeanGroupKey<K> key, MarshalledValue<Map<K, V>, C> value) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Received pre-passivate event for bean group %s", key.getId());
        try {
            Map<K, V> instances = value.get(this.context);
            List<V> passivated = new ArrayList<>(instances.size());
            try {
                for (V instance : instances.values()) {
                    K id = instance.getId();
                    InfinispanEjbLogger.ROOT_LOGGER.tracef("Invoking pre-passivate callback for bean %s", id);
                    instance.prePassivate();
                    this.passivations.computeIfAbsent(instance.getName(), COUNTER_FACTORY).updateAndGet(INCREMENT);
                    passivated.add(instance);
                    // Cascade eviction to creation meta data entry
                    this.executor.execute(() -> this.cache.evict(new InfinispanBeanMetaDataKey<>(id)));
                }
            } catch (RuntimeException | Error e) {
                // Restore state of pre-passivated beans
                for (V instance : passivated) {
                    InfinispanEjbLogger.ROOT_LOGGER.tracef("Invoking post-activate callback for bean %s", instance.getId());
                    try {
                        instance.postActivate();
                    } catch (RuntimeException | Error t) {
                        InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                }
                // Abort passivation if any beans failed to pre-passivate
                throw e;
            }
        } catch (IOException e) {
            InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }
}
