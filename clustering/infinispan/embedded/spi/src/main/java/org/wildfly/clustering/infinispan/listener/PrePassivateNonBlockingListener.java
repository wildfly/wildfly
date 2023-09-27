/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import java.util.function.BiConsumer;

import org.infinispan.Cache;

/**
 * Generic non-blocking pre-passivation listener that delegates to a non-blocking consumer.
 * @author Paul Ferraro
 */
public class PrePassivateNonBlockingListener<K, V> extends CacheEventListenerRegistrar<K, V> {

    public PrePassivateNonBlockingListener(Cache<K, V> cache, BiConsumer<K, V> consumer) {
        super(cache, new PrePassivateListener<>(new NonBlockingCacheEventListener<>(consumer)));
    }
}
