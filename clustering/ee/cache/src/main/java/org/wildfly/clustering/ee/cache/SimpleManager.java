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
