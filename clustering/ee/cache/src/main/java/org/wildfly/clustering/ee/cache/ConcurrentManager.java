/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.cache;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<K, Map.Entry<Integer, V>> objects = new ConcurrentHashMap<>();
    private final BiFunction<K, Map.Entry<Integer, V>, Map.Entry<Integer, V>> addFunction = new BiFunction<K, Map.Entry<Integer, V>, Map.Entry<Integer, V>>() {
        @Override
        public Map.Entry<Integer, V> apply(K id, Map.Entry<Integer, V> entry) {
            return entry != null ? new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(entry.getKey().intValue() + 1), entry.getValue()) : new VolatileEntry<>(new Integer(0));
        }
    };
    private final Consumer<V> createTask;
    private final BiFunction<K, Map.Entry<Integer, V>, Map.Entry<Integer, V>> removeFunction;

    public ConcurrentManager(Consumer<V> createTask, Consumer<V> closeTask) {
        this.createTask = createTask;
        this.removeFunction = new BiFunction<K, Map.Entry<Integer, V>, Map.Entry<Integer, V>>() {
            @Override
            public Map.Entry<Integer, V> apply(K key, Map.Entry<Integer, V> entry) {
                int count = entry.getKey().intValue();
                V value = entry.getValue();
                if (count == 0) {
                    if (value != null) {
                        closeTask.accept(value);
                    }
                    // Returning null will remove the map entry
                    return null;
                }
                return new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(count - 1), value);
            }
        };
    }

    @Override
    public V apply(K key, Function<Runnable, V> factory) {
        Map.Entry<Integer, V> entry = this.objects.compute(key, this.addFunction);
        if (entry.getValue() == null) {
            synchronized (entry) {
                if (entry.getValue() == null) {
                    Map<K, Map.Entry<Integer, V>> objects = this.objects;
                    BiFunction<K, Map.Entry<Integer, V>, Map.Entry<Integer, V>> removeFunction = this.removeFunction;
                    Runnable closeTask = new Runnable() {
                        @Override
                        public void run() {
                            objects.compute(key, removeFunction);
                        }
                    };
                    V value = factory.apply(closeTask);
                    if (value != null) {
                        this.createTask.accept(value);
                        entry.setValue(value);
                    } else {
                        closeTask.run();
                    }
                }
            }
        }
        return entry.getValue();
    }

    /**
     * Like {@link AbstractMap.SimpleEntry}, but uses a volatile value reference, which lets us use double checked locking to lazily initialize value.
     * @param <K> the entry key type
     * @param <V> the entry value type
     */
    private static class VolatileEntry<K, V> implements Map.Entry<K, V> {

        private final K key;
        private volatile V value;

        VolatileEntry(K key) {
            this(key, null);
        }

        VolatileEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            V previous = this.value;
            this.value = value;
            return previous;
        }
    }
}
