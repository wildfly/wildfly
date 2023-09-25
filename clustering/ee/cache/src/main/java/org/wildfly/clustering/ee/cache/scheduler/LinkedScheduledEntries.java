/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.cache.scheduler;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * {@link ScheduledEntries} implemented using a {@link ConcurrentDirectDeque}.
 * Both {@link #add(Object, Object)} and {@link #remove(Object)} run in O(1) time.
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
    public boolean contains(K key) {
        return this.tokens.containsKey(key);
    }

    @Override
    public Map.Entry<K, V> peek() {
        return this.queue.peekFirst();
    }

    @Override
    public Stream<Map.Entry<K, V>> stream() {
        return this.queue.stream();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        Iterator<Map.Entry<K, V>> iterator = this.queue.iterator();
        Map<K, Object> tokens = this.tokens;
        return new Iterator<>() {
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
    public String toString() {
        return this.queue.toString();
    }
}
