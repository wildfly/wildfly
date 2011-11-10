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
package org.jboss.as.ejb3.cache.impl.backing;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntry;

/**
 * Basic {@link BasicCacheEntry} implementation for use with a non-passivating {@link BackingCache}. A wrapper for the
 * {@link Cacheable} to allow it to be managed by the backing cache .
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class NonPassivatingBackingCacheEntry<K extends Serializable, V extends Cacheable<K>> extends AbstractBackingCacheEntry<K, V> implements BackingCacheEntry<K, V> {
    /** The serialVersionUID */
    private static final long serialVersionUID = 1325918596862109742L;

    private final V wrapped;
    private final ReentrantLock lock = new ReentrantLock();
    // guarded by lock
    private volatile boolean valid = true;

    /**
     * Create a new SimpleBackingCacheEntry.
     *
     * @param wrapped the item to wrap
     */
    public NonPassivatingBackingCacheEntry(V wrapped) {
        this.wrapped = wrapped;
    }

    // -------------------------------------------------------- BackingCacheEntry

    @Override
    public V getUnderlyingItem() {
        return wrapped;
    }

    @Override
    public boolean isModified() {
        return wrapped.isModified();
    }

    /**
     * {@inheritDoc}
     *
     * @return the id of the {@link BackingCacheEntry#getUnderlyingItem() underlying item}. Cannot be <code>null</code>.
     */
    @Override
    public K getId() {
        return wrapped.getId();
    }

    @Override
    public void lock() {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw EjbMessages.MESSAGES.lockAcquisitionInterrupted(e, this.wrapped.getId());
        }
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public void invalidate() {
        this.valid = false;
    }

    @Override
    public boolean isValid() {
        return valid;
    }
}
