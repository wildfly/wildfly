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

package org.jboss.as.clustering.infinispan.cache;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.commons.api.BasicCache;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A cache that resolves itself from its cache container lazily.
 * @author Paul Ferraro
 */
public abstract class LazyBasicCache<K, V, C extends BasicCache<K, V>> implements BasicCache<K, V>, Supplier<C>, PrivilegedAction<C> {

    private final String name;

    protected LazyBasicCache(String name) {
        this.name = name;
    }

    @Override
    public C get() {
        return WildFlySecurityManager.doUnchecked(this);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void start() {
        this.get().start();
    }

    @Override
    public void stop() {
        this.get().stop();
    }

    @Override
    public String getVersion() {
        return this.get().getVersion();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.get().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.get().containsValue(value);
    }

    @Override
    public boolean isEmpty() {
        return this.get().isEmpty();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.get().entrySet();
    }

    @Override
    public Set<K> keySet() {
        return this.get().keySet();
    }

    @Override
    public Collection<V> values() {
        return this.get().values();
    }

    @Override
    public void clear() {
        this.get().clear();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return this.get().compute(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().compute(key, remappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().compute(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return this.get().computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().computeIfAbsent(key, mappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().computeIfAbsent(key, mappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return this.get().computeIfPresent(key, remappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().computeIfPresent(key, remappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().computeIfPresent(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public V get(Object key) {
        return this.get().get(key);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return this.get().merge(key, value, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().merge(key, value, remappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().merge(key, value, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public V put(K key, V value) {
        return this.get().put(key, value);
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit) {
        return this.get().put(key, value, lifespan, lifespanUnit);
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().put(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        this.get().putAll(map);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit) {
        this.get().putAll(map, lifespan, lifespanUnit);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        this.get().putAll(map, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return this.get().putIfAbsent(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit) {
        return this.get().putIfAbsent(key, value, lifespan, lifespanUnit);
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().putIfAbsent(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public V remove(Object key) {
        return this.get().remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return this.get().remove(key, value);
    }

    @Override
    public V replace(K key, V value) {
        return this.get().replace(key, value);
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit) {
        return this.get().replace(key, value, lifespan, lifespanUnit);
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().replace(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return this.get().replace(key, oldValue, newValue);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit) {
        return this.get().replace(key, oldValue, newValue, lifespan, lifespanUnit);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().replace(key, oldValue, newValue, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public int size() {
        return this.get().size();
    }

    @Override
    public CompletableFuture<Void> clearAsync() {
        return this.get().clearAsync();
    }

    @Override
    public CompletableFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return this.get().computeAsync(key, remappingFunction);
    }

    @Override
    public CompletableFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().computeAsync(key, remappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().computeAsync(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction) {
        return this.get().computeIfAbsentAsync(key, mappingFunction);
    }

    @Override
    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().computeIfAbsentAsync(key, mappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().computeIfAbsentAsync(key, mappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return this.get().computeIfPresentAsync(key, remappingFunction);
    }

    @Override
    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().computeIfPresentAsync(key, remappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().computeIfPresentAsync(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> getAsync(K key) {
        return this.get().getAsync(key);
    }

    @Override
    public CompletableFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return this.get().mergeAsync(key, value, remappingFunction);
    }

    @Override
    public CompletableFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
        return this.get().mergeAsync(key, value, remappingFunction, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().mergeAsync(key, value, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> putAsync(K key, V value) {
        return this.get().putAsync(key, value);
    }

    @Override
    public CompletableFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit) {
        return this.get().putAsync(key, value, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().putAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> entries) {
        return this.get().putAllAsync(entries);
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> entries, long lifespan, TimeUnit lifespanUnit) {
        return this.get().putAllAsync(entries, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> entries, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().putAllAsync(entries, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> putIfAbsentAsync(K key, V value) {
        return this.get().putIfAbsentAsync(key, value);
    }

    @Override
    public CompletableFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit) {
        return this.get().putIfAbsentAsync(key, value, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().putIfAbsentAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> removeAsync(Object key) {
        return this.get().removeAsync(key);
    }

    @Override
    public CompletableFuture<Boolean> removeAsync(Object key, Object value) {
        return this.get().removeAsync(key, value);
    }

    @Override
    public CompletableFuture<V> replaceAsync(K key, V value) {
        return this.get().replaceAsync(key, value);
    }

    @Override
    public CompletableFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit) {
        return this.get().replaceAsync(key, value, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().replaceAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
        return this.get().replaceAsync(key, oldValue, newValue);
    }

    @Override
    public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit) {
        return this.get().replaceAsync(key, oldValue, newValue, lifespan, lifespanUnit);
    }

    @Override
    public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.get().replaceAsync(key, oldValue, newValue, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<Long> sizeAsync() {
        return this.get().sizeAsync();
    }
}
