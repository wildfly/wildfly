/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.cache.spi.impl;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;

/**
 * Encapsulation of the {@link Identifiable#getId() id} and {@link BackingCacheEntry#getLastUsed() last used timestamp} of a
 * cached {@link BackingCacheEntry}.
 * <p>
 * Implements <code>Comparable</code> to make it easy to sort for LRU comparisons.
 * </p>
 *
 * @see AbstractBackingCacheEntryStore#getInMemoryEntries()
 * @see AbstractBackingCacheEntryStore#getAllEntries()
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class CacheableTimestamp<K extends Serializable> implements Identifiable<K>, Comparable<CacheableTimestamp<K>> {
    private K id;
    private long lastUsed;

    public CacheableTimestamp(BackingCacheEntry<K, ?> entry) {
        this(entry.getId(), entry.getLastUsed());
    }

    public CacheableTimestamp(K id, long lastUsed) {
        this.id = id;
        this.lastUsed = lastUsed;
    }

    @Override
    public K getId() {
        return id;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    /**
     * Compares based on {@link #getLastUsed() last used}, returning -1 for earlier timestamps.
     */
    @Override
    public int compareTo(CacheableTimestamp<K> o) {
        if (this.lastUsed < o.lastUsed)
            return -1;
        else if (this.lastUsed > o.lastUsed)
            return 1;
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof CacheableTimestamp) {
            return this.id.equals(((CacheableTimestamp<K>) obj).id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
