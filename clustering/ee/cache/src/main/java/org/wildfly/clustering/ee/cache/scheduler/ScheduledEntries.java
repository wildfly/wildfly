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

import java.util.Iterator;
import java.util.Map;

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
     * Returns, but does not remove, the first entry.
     */
    default Map.Entry<K, V> peek() {
        Iterator<Map.Entry<K, V>> entries = this.iterator();
        return entries.hasNext() ? entries.next() : null;
    }
}