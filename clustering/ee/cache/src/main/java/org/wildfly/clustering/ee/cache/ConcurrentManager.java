/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.wildfly.clustering.ee.Manager;

/**
 * Manages creation and destruction of values to be shared across threads.
 * @author Paul Ferraro
 * @param <K> the key type
 * @param <V> the type of the managed value
 */
public class ConcurrentManager<K, V> implements Manager<K, V> {

    private final Map<K, Map.Entry<Integer, AtomicReference<V>>> objects = new ConcurrentHashMap<>();
    private final BiFunction<K, Map.Entry<Integer, AtomicReference<V>>, Map.Entry<Integer, AtomicReference<V>>> addFunction = new BiFunction<>() {
        @Override
        public Map.Entry<Integer, AtomicReference<V>> apply(K id, Map.Entry<Integer, AtomicReference<V>> entry) {
            int count = (entry != null) ? entry.getKey() + 1 : 0;
            AtomicReference<V> reference = (entry != null) ? entry.getValue() : new AtomicReference<>();
            return new AbstractMap.SimpleImmutableEntry<>(count, reference);
        }
    };
    private final Consumer<V> createTask;
    private final BiFunction<K, Map.Entry<Integer, AtomicReference<V>>, Map.Entry<Integer, AtomicReference<V>>> removeFunction;

    public ConcurrentManager(Consumer<V> createTask, Consumer<V> closeTask) {
        this.createTask = createTask;
        this.removeFunction = new BiFunction<>() {
            @Override
            public Map.Entry<Integer, AtomicReference<V>> apply(K key, Map.Entry<Integer, AtomicReference<V>> entry) {
                // Entry can be null if entry was already removed, i.e. managed object was already closed
                int count = (entry != null) ? entry.getKey() : 0;
                AtomicReference<V> reference = (entry != null) ? entry.getValue() : null;
                if (count == 0) {
                    V value = (reference != null) ? reference.get() : null;
                    if (value != null) {
                        closeTask.accept(value);
                    }
                    // Returning null will remove the map entry
                    return null;
                }
                return new AbstractMap.SimpleImmutableEntry<>(count - 1, reference);
            }
        };
    }

    @Override
    public V apply(K key, Function<Runnable, V> factory) {
        Map.Entry<Integer, AtomicReference<V>> entry = this.objects.compute(key, this.addFunction);
        AtomicReference<V> reference = entry.getValue();
        if (reference.get() == null) {
            synchronized (reference) {
                if (reference.get() == null) {
                    Map<K, Map.Entry<Integer, AtomicReference<V>>> objects = this.objects;
                    BiFunction<K, Map.Entry<Integer, AtomicReference<V>>, Map.Entry<Integer, AtomicReference<V>>> removeFunction = this.removeFunction;
                    Runnable closeTask = new Runnable() {
                        @Override
                        public void run() {
                            objects.compute(key, removeFunction);
                        }
                    };
                    V value = factory.apply(closeTask);
                    if (value != null) {
                        this.createTask.accept(value);
                        reference.set(value);
                    } else {
                        closeTask.run();
                    }
                }
            }
        }
        return reference.get();
    }
}
