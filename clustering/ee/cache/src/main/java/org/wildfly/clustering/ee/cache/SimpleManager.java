/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.wildfly.clustering.ee.Manager;

/**
 * Manages creation and destruction of objects not sharable across threads.
 * @author Paul Ferraro
 * @param <K> the key type
 * @param <V> the type of the managed value
 */
public class SimpleManager<K, V> implements Manager<K, V> {

    private final Consumer<V> createTask;
    private final Consumer<V> closeTask;

    public SimpleManager(Consumer<V> createTask, Consumer<V> closeTask) {
        this.createTask = createTask;
        this.closeTask = closeTask;
    }

    @Override
    public V apply(K key, Function<Runnable, V> factory) {
        Map.Entry<K, V> entry = new AbstractMap.SimpleEntry<>(key, null);
        Consumer<V> closeTask = this.closeTask;
        V value = factory.apply(new Runnable() {
            @Override
            public void run() {
                closeTask.accept(entry.getValue());
            }
        });
        if (value != null) {
            entry.setValue(value);
            this.createTask.accept(value);
        }
        return value;
    }
}
