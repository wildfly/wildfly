/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import javax.ejb.NoSuchEJBException;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCache;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;

/**
 * @author Paul Ferraro
 *
 */
public abstract class AbstractCache<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>> implements Cache<K, V> {
    private final BackingCache<K, V, E> backingCache;

    protected AbstractCache(BackingCache<K, V, E> backingCache) {
        this.backingCache = backingCache;
    }

    @Override
    public V create() {
        return this.backingCache.create().getUnderlyingItem();
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.Cache#discard(java.io.Serializable)
     */
    @Override
    public void discard(K key) {
        this.backingCache.discard(key);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.Cache#get(java.io.Serializable)
     */
    @Override
    public V get(K key) throws NoSuchEJBException {
        E entry = this.backingCache.get(key);
        return (entry != null) ? entry.getUnderlyingItem() : null;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.Cache#release(org.jboss.as.ejb3.cache.Identifiable)
     */
    @Override
    public void release(V object) {
        this.backingCache.release(object.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(K key) {
        this.backingCache.remove(key);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.Cache#start()
     */
    @Override
    public void start() {
        this.backingCache.start();
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.ejb3.cache.Cache#stop()
     */
    @Override
    public void stop() {
        this.backingCache.stop();
    }
}
