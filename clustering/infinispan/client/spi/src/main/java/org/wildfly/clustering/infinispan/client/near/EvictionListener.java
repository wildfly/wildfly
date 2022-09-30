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
