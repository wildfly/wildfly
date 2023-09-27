/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.cache.scheduler;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * A collection of scheduled entries with a predictable iteration order.
 * @author Paul Ferraro
 */
public interface ScheduledEntries<K, V> extends Iterable<Map.Entry<K, V>> {

    /**
     * Indicates whether the entries are sorted, or if iteration order recapitulates insertion order.
     * @return true, if these entries are sorted, false otherwise.
     */
    boolean isSorted();

    /**
     * Adds an entry using the specified key and value.
     * @param key an entry key
     * @param value an entry value
     */
    void add(K key, V value);

    /**
     * Removes the entry with the specified key.
     * @param key an entry key
     */
    void remove(K key);

    /**
     * Indicates whether specified key exists among the scheduled entries.
     * @param key an entry key
     * @return true, if the key is a scheduled entry, false otherwise
     */
    boolean contains(K key);

    /**
     * Returns, but does not remove, the first entry.
     */
    default Map.Entry<K, V> peek() {
        return this.stream().findFirst().orElse(null);
    }

    /**
     * Returns a stream of scheduled entries.
     * @return a stream of scheduled entries.
     */
    Stream<Map.Entry<K, V>> stream();

    @Override
    default Iterator<Entry<K, V>> iterator() {
        return this.stream().iterator();
    }
}