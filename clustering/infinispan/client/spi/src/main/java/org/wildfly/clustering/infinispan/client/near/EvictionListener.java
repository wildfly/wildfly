/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.near;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.MetadataValue;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

/**
 * Removal listener that triggers the specified listener when the removal cause is {@link RemovalCause#SIZE}.
 * @author Paul Ferraro
 */
public class EvictionListener<K, V> implements RemovalListener<K, MetadataValue<V>>, Consumer<Cache<K, MetadataValue<V>>> {

    private final BiConsumer<K, MetadataValue<V>> defaultListener;
    private final BiConsumer<Cache<Object, MetadataValue<Object>>, Map.Entry<Object, Object>> listener;
    private Cache<Object, MetadataValue<Object>> cache;

    public EvictionListener(BiConsumer<K, MetadataValue<V>> defaultListener, BiConsumer<Cache<Object, MetadataValue<Object>>, Map.Entry<Object, Object>> listener) {
        this.defaultListener = defaultListener;
        this.listener = listener;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(Cache<K, MetadataValue<V>> cache) {
        this.cache = (Cache<Object, MetadataValue<Object>>) (Cache<?, ?>) cache;
    }

    @Override
    public void onRemoval(K key, MetadataValue<V> value, RemovalCause cause) {
        this.defaultListener.accept(key, value);
        if (cause == RemovalCause.SIZE) {
            this.listener.accept(this.cache, Map.entry(key, value.getValue()));
        }
    }
}
