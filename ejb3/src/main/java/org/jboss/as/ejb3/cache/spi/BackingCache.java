/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

import javax.ejb.NoSuchEJBException;

import org.jboss.as.ejb3.cache.AffinitySupport;
import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.Removable;

/**
 * An internal cache to which an external-facing {@link Cache} delegates, either directly or indirectly.
 * <p>
 * There are two key distinctions between a BackingCache and the external-facing Cache:
 * <ol>
 * <li>
 * A Cache directly handles external classes that implement the limited {@link Cacheable} interface. CacheItem is deliberately
 * limited to avoid placing a implementation burden on external classes. A BackingCache works with instances of the more
 * expressive internal interface {@link BackingCacheEntry}, and thus can directly implement more complex functionality.</li>
 * <li>
 * A BackingCache does not attempt to control concurrent client access to its cached {@link BackingCacheEntry} instances, beyond
 * the simple act of {@link BackingCacheEntry#setInUse(boolean) marking the entries as being in or out of use}. It assumes the
 * external-facing Cache is preventing concurrent access to a given entry and is properly coordinating calls to the backing
 * cache.</li>
 * </ol>
 * </p>
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public interface BackingCache<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>> extends Removable<K>, AffinitySupport<K> {
    /**
     * Creates and caches a new instance of <code>C</code>, wrapped by a new <code>T</code>. The new <code>T</code> *is*
     * returned, but is not regarded as being "in use". Callers *must not* attempt to use the underlying <code>C</code> without
     * first calling {@link #get(Object)}.
     *
     * @return the new <code>T</code>
     */
    E create();

    /**
     * Get the specified object from cache. This will mark the entry as being in use.
     *
     * @param key the identifier of the object
     * @return the object
     * @throws NoSuchEJBException if the object does not exist
     */
    E get(K key) throws NoSuchEJBException;

    /**
     * Peek at an object which might be in use. Does not change the status of the item in terms of whether it is in use.
     *
     * @param key the identifier of the object
     * @return the object
     * @throws NoSuchEJBException if the object does not exist
     */
    E peek(K key) throws NoSuchEJBException;

    /**
     * Release the object from use.
     *
     * @param key the identifier of the object
     *
     * @return the entry that was released
     */
    E release(K key);

    /**
     * Discard the specified object from cache.
     *
     * @param key the identifier of the object
     */
    void discard(K key);

    /**
     * Start the cache.
     */
    void start();

    /**
     * Stop the cache.
     */
    void stop();

    /**
     * Gets whether this cache supports clustering functionality.
     *
     * @return <code>true</code> if clustering is supported, <code>false</code> otherwise
     */
    boolean isClustered();

    /**
     * Registers a listener for callbacks when the cache starts and stops.
     *
     * @param listener the listener. Cannot be <code>null</code>.
     */
    void addLifecycleListener(BackingCacheLifecycleListener listener);

    /**
     * Removes a registered lifecycle listener.
     *
     * @param listener the listener. Cannot be <code>null</code>.
     */
    void removeLifecycleListener(BackingCacheLifecycleListener listener);
}
