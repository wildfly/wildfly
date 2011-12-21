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

package org.jboss.as.ejb3.cache.spi;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;

/**
 * An in-memory store for {@link BackingCacheEntry} instances that integrates a persistent store and the ability to use its
 * knowledge of when objects are accessed to coordinate the passivation and expiration of cached objects. Note that this class
 * does NOT call any callbacks; it performs passivation and expiration by invoking methods on the
 * {@link #setPassivatingCache(PassivatingBackingCache) injected backing cache}; the cache performs the actual passivation or
 * removal.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public interface BackingCacheEntryStore<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>>
    extends GroupCompatibilityChecker {
    /**
     * Put a new entry into the store. This operation should only be performed once per entry.
     *
     * @param entry the object to store. Cannot be <code>null</code>.
     *
     * @throws IllegalStateException if the store is already managing an entry with the same {@link Identifiable#getId() id}. It
     *         is not a requirement that the store throw this exception in this case, but it is permissible. This basically puts
     *         the onus on callers to ensure this operation is only performed once per entry.
     */
    void insert(E entry);

    /**
     * Gets the entry with the given id from the store.
     *
     * @param key {@link Identifiable#getId() id} of the entry. Cannot be <code>null</code>.
     * @return the object store under <code>id</code>. May return <code>null</code>.
     */
    E get(K key, boolean lock);

    /**
     * Update an already cached item.
     *
     * @param entry the entry to update
     * @param modified was the entry modified since {@link #get(Object)} was called?
     *
     * @throws IllegalStateException if the store isn't already managing an entry with the same {@link Identifiable#getId() id}.
     *         It is not a requirement that the store throw this exception in this case, but it is permissible. This basically
     *         puts the onus on callers to ensure {@link #insert(E)} is invoked before the first replication.
     */
    void update(E entry, boolean modified);

    /**
     * Remove the object with the given key from the store.
     *
     * @param key {@link Identifiable#getId() id} of the entry. Cannot be <code>null</code>.
     *
     * @return the object that was cached under <code>key</code>
     */
    E remove(K key);

    /**
     * Remove the entry with the given key from any in-memory store while retaining it in the persistent store.
     *
     * @param entry the entry to passivate
     */
    void passivate(E entry);

    /**
     * Gets whether this store supports clustering functionality.
     *
     * @return <code>true</code> if clustering is supported, <code>false</code> otherwise
     */
    boolean isClustered();

    /**
     * Perform any initialization work.
     */
    void start();

    /**
     * Perform any shutdown work.
     */
    void stop();

    BackingCacheEntryStoreConfig getConfig();

    StatefulTimeoutInfo getTimeout();
}
