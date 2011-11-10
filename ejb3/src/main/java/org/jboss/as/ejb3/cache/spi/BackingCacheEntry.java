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

import org.jboss.as.ejb3.cache.Cacheable;

/**
 * An object that can be managed by a {@link BackingCache}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public interface BackingCacheEntry<K extends Serializable, V extends Cacheable<K>> extends Cacheable<K>, Comparable<BackingCacheEntry<K, V>> {
    /**
     * Gets the underlying CacheItem.
     *
     * @return
     */
    V getUnderlyingItem();

    /**
     * Gets whether this entry is in use by a caller.
     */
    boolean isInUse();

    /**
     * Sets whether this entry is in use by a caller.
     *
     * @param inUse
     */
    void setInUse(boolean inUse);

    /**
     * Gets the timestamp of the last time this entry was in use.
     *
     * @return
     */
    long getLastUsed();

    /**
     * Gets whether the entry can be passivated without invoking any callbacks on the underlying item.
     */
    boolean isPrePassivated();

    /**
     * Sets whether the entry can be passivated without invoking any callbacks on the underlying item.
     */
    void setPrePassivated(boolean prePassivated);

    /**
     * Attempt to lock this item, failing promptly if the lock is already held by another thread. Has the same semantics as
     * {@java.util.concurrent.ReentrantLock#tryLock()}.
     *
     * @return <code>true</code> if the lock was acquired, <code>false</code> otherwise
     */
    boolean tryLock();

    /**
     * Lock this item, blocking until the lock is acquired. Has the same semantics as
     * {@java.util.concurrent.ReentrantLock#lockInterruptibly()},
     * except that a <code>RuntimeException</code> will be thrown if the thread is interrupted instead of
     * <code>InterruptedException</code>.
     */
    void lock();

    /**
     * Unlock this item. Has the same semantics as {@java.util.concurrent.ReentrantLock#unlock()}.
     */
    void unlock();

    /**
     * Whether this entry has been invalidated (in which case it should be reacquired).
     * <p>
     * <strong>NOTE:</strong> This method should only be called with the lock held.
     * </p>
     *
     * @return <code>true</code>e if still valid, <code>false</code>e if invalidated
     *
     * @see #invalidate()
     */
    boolean isValid();

    /**
     * Causes {@link #isValid()} to hereafter return <code>true</code>.
     * <p>
     * <strong>NOTE:</strong> This method should only be called with the lock held.
     * </p>
     */
    void invalidate();
}
