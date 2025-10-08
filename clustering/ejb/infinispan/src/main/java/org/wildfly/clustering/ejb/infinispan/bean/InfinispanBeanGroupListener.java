/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.embedded.listener.ListenerRegistration;
import org.wildfly.clustering.cache.infinispan.embedded.listener.PostActivateBlockingListener;
import org.wildfly.clustering.cache.infinispan.embedded.listener.PrePassivateBlockingListener;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.server.Registration;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupListener<K, V extends BeanInstance<K>, C> implements Registration {

    private final Cache<Key<K>, ?> cache;
    private final C context;
    private final Executor executor;
    private final ListenerRegistration postActivateListenerRegistration;
    private final ListenerRegistration prePassivateListenerRegistration;

    public InfinispanBeanGroupListener(EmbeddedCacheConfiguration configuration, C context) {
        this.cache = configuration.getCache();
        this.context = context;
        // We only need to listen for activation/passivation events for non-persistent caches
        // pre-passivate/post-activate callbacks for persistent caches are triggered via GroupManager
        this.executor = !configuration.getCacheProperties().isPersistent() ? configuration.getExecutor() : null;
        this.postActivateListenerRegistration = (this.executor != null) ? new PostActivateBlockingListener<>(configuration.getCache(), this::postActivate).register(BeanGroupKey.class) : null;
        this.prePassivateListenerRegistration = (this.executor != null) ? new PrePassivateBlockingListener<>(configuration.getCache(), this::prePassivate).register(BeanGroupKey.class) : null;
    }

    @Override
    public void close() {
        Optional.ofNullable(this.postActivateListenerRegistration).ifPresent(Consumer.close());
        Optional.ofNullable(this.prePassivateListenerRegistration).ifPresent(Consumer.close());
    }

    void postActivate(BeanGroupKey<K> key, MarshalledValue<Map<K, V>, C> value) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Received post-activate event for bean group %s", key.getId());
        try {
            Map<K, V> instances = value.get(this.context);
            for (V instance : instances.values()) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Invoking post-activate callback for bean %s", instance.getId());
                instance.postActivate();
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
