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
import java.util.function.Supplier;

import org.wildfly.clustering.ee.Manager;

/**
 * Manages creation and destruction of values to be shared across threads.
 * @author Paul Ferraro
 * @param <K> the key type
 * @param <V> the type of the managed value
 */
public class ConcurrentManager<K, V> implements Manager<K, V> {

    private final Map<K, Map.Entry<Integer, VolatileReference<V>>> objects = new ConcurrentHashMap<>();
    private final BiFunction<K, Map.Entry<Integer, VolatileReference<V>>, Map.Entry<Integer, VolatileReference<V>>> addFunction = new BiFunction<K, Map.Entry<Integer, VolatileReference<V>>, Map.Entry<Integer, VolatileReference<V>>>() {
        @Override
        public Map.Entry<Integer, VolatileReference<V>> apply(K id, Map.Entry<Integer, VolatileReference<V>> entry) {
            Integer count = (entry != null) ? Integer.valueOf(entry.getKey().intValue() + 1) : new Integer(0);
            VolatileReference<V> reference = (entry != null) ? entry.getValue() : new VolatileReference<>();
            return new AbstractMap.SimpleImmutableEntry<>(count, reference);
        }
    };
    private final Consumer<V> createTask;
    private final BiFunction<K, Map.Entry<Integer, VolatileReference<V>>, Map.Entry<Integer, VolatileReference<V>>> removeFunction;

    public ConcurrentManager(Consumer<V> createTask, Consumer<V> closeTask) {
        this.createTask = createTask;
        this.removeFunction = new BiFunction<K, Map.Entry<Integer, VolatileReference<V>>, Map.Entry<Integer, VolatileReference<V>>>() {
            @Override
            public Map.Entry<Integer, VolatileReference<V>> apply(K key, Map.Entry<Integer, VolatileReference<V>> entry) {
                int count = entry.getKey().intValue();
                VolatileReference<V> reference = entry.getValue();
                if (count == 0) {
                    V value = reference.get();
                    if (value != null) {
                        closeTask.accept(value);
                    }
                    // Returning null will remove the map entry
                    return null;
                }
                return new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(count - 1), reference);
            }
        };
    }

    @Override
    public V apply(K key, Function<Runnable, V> factory) {
        Map.Entry<Integer, VolatileReference<V>> entry = this.objects.compute(key, this.addFunction);
        VolatileReference<V> reference = entry.getValue();
        if (reference.get() == null) {
            synchronized (reference) {
                if (reference.get() == null) {
                    Map<K, Map.Entry<Integer, VolatileReference<V>>> objects = this.objects;
                    BiFunction<K, Map.Entry<Integer, VolatileReference<V>>, Map.Entry<Integer, VolatileReference<V>>> removeFunction = this.removeFunction;
                    Runnable closeTask = new Runnable() {
                        @Override
                        public void run() {
                            objects.compute(key, removeFunction);
                        }
                    };
                    V value = factory.apply(closeTask);
                    if (value != null) {
                        this.createTask.accept(value);
                        reference.accept(value);
                    } else {
                        closeTask.run();
                    }
                }
            }
        }
        return reference.get();
    }

    public static class VolatileReference<V> implements Supplier<V>, Consumer<V> {
        private volatile V value = null;

        @Override
        public void accept(V value) {
            this.value = value;
        }

        @Override
        public V get() {
            return this.value;
        }
    }
}
