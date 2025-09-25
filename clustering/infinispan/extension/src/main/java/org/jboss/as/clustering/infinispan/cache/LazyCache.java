/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.cache;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheCollection;
import org.infinispan.CacheSet;
import org.infinispan.commons.IllegalLifecycleStateException;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;

/**
 * An embedded cache that resolves itself from its cache container lazily.
 * @author Paul Ferraro
 */
public class LazyCache<K, V> extends LazyBasicCache<K, V, Cache<K, V>> implements Cache<K, V> {

    private final EmbeddedCacheManager container;
    private final String name;

    public LazyCache(EmbeddedCacheManager container, String name) {
        super(name);
        this.container = container;
        this.name = name;
    }

    @Override
    public Cache<K, V> run() {
        return Optional.ofNullable(this.container.<K, V>getCache(this.name, false)).orElseThrow(IllegalLifecycleStateException::new);
    }

    @Override
    public EmbeddedCacheManager getCacheManager() {
        return this.container;
    }

    @Override
    public AdvancedCache<K, V> getAdvancedCache() {
        return this.get().getAdvancedCache();
    }

    @Override
    public Configuration getCacheConfiguration() {
        return this.get().getCacheConfiguration();
    }

    @Override
    public ComponentStatus getStatus() {
        return this.get().getStatus();
    }

    @Override
    public boolean startBatch() {
        return this.get().startBatch();
    }

    @Override
    public void endBatch(boolean successful) {
        this.get().endBatch(successful);
    }

    @Override
    public CacheSet<Entry<K, V>> entrySet() {
        return this.get().entrySet();
    }

    @Override
    public CacheSet<K> keySet() {
        return this.get().keySet();
    }

    @Override
    public CacheCollection<V> values() {
        return this.get().values();
    }

    @Override
    public void evict(K key) {
        this.get().evict(key);
    }

    @Override
    public void putForExternalRead(K key, V value) {
        this.get().putForExternalRead(key, value);
    }

    @Override
    public void putForExternalRead(K key, V value, long lifespan, TimeUnit lifespanUnit) {
        this.get().putForExternalRead(key, value, lifespan, lifespanUnit);
    }

    @Override
    public void putForExternalRead(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        this.get().putForExternalRead(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletionStage<Void> addListenerAsync(Object listener) {
        return this.get().addListenerAsync(listener);
    }

    @Override
    public <C> CompletionStage<Void> addListenerAsync(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
        return this.get().addListenerAsync(listener, filter, converter);
    }

    @Override
    public <C> CompletionStage<Void> addFilteredListenerAsync(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
        return this.get().addFilteredListenerAsync(listener, filter, converter, filterAnnotations);
    }

    @Override
    public <C> CompletionStage<Void> addStorageFormatFilteredListenerAsync(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
        return this.get().addStorageFormatFilteredListenerAsync(listener, filter, converter, filterAnnotations);
    }

    @Override
    public CompletionStage<Void> removeListenerAsync(Object listener) {
        return this.get().removeListenerAsync(listener);
    }
}
