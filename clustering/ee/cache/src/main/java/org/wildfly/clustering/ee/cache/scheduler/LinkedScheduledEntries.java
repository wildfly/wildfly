/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ee.cache.scheduler;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * {@link ScheduledEntries} implemented using a {@link ConcurrentDirectDeque}.
 * Both {@link #add(Object, Comparable)} and {@link #remove(Object)} run in O(1) time.
 * @author Paul Ferraro
 */
public class LinkedScheduledEntries<K, V> implements ScheduledEntries<K, V> {
    private final ConcurrentDirectDeque<Map.Entry<K, V>> queue = ConcurrentDirectDeque.newInstance();
    private final Map<K, Object> tokens = new ConcurrentHashMap<>();

    @Override
    public boolean isSorted() {
        return false;
    }

    @Override
    public void add(K key, V value) {
        Object token = this.queue.offerLastAndReturnToken(new SimpleImmutableEntry<>(key, value));
        this.tokens.put(key, token);
    }

    @Override
    public void remove(K key) {
        Object token = this.tokens.remove(key);
        if (token != null) {
            this.queue.removeToken(token);
        }
    }

    @Override
    public Map.Entry<K, V> peek() {
        return this.queue.peekFirst();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        Iterator<Map.Entry<K, V>> iterator = this.queue.iterator();
        Map<K, Object> tokens = this.tokens;
        return new Iterator<Map.Entry<K, V>>() {
            private K current = null;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Map.Entry<K, V> next() {
                Map.Entry<K, V> next = iterator.next();
                this.current = next.getKey();
                return next;
            }

            @Override
            public void remove() {
                iterator.remove();
                tokens.remove(this.current);
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<K, V>> action) {
        this.queue.forEach(action);
    }

    @Override
    public Spliterator<Map.Entry<K, V>> spliterator() {
        return this.queue.spliterator();
    }
}
