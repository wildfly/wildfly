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
package org.jboss.as.ejb3.cache.spi.impl;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;

/**
 * Abstract superclass for {@link BackingCacheEntry} implementations.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public abstract class AbstractBackingCacheEntry<K extends Serializable, V extends Cacheable<K>> implements BackingCacheEntry<K, V> {
    /** The serialVersionUID */
    private static final long serialVersionUID = 4562025672441864736L;

    private volatile long lastUsed = System.currentTimeMillis();
    private transient volatile boolean inUse;
    private volatile boolean prePassivated;
    private transient volatile boolean invalid;

    @Override
    public long getLastUsed() {
        return lastUsed;
    }

    @Override
    public boolean isInUse() {
        return inUse;
    }

    @Override
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
        setLastUsed(System.currentTimeMillis());
    }

    /**
     * Exposed as public only as an aid to unit tests. In normal use this should only be invoked internally or by subclasses.
     *
     * @param lastUsed time since epoch when last used
     */
    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    @Override
    public boolean isPrePassivated() {
        return prePassivated;
    }

    @Override
    public void setPrePassivated(boolean prePassivated) {
        this.prePassivated = prePassivated;
    }

    @Override
    public void invalidate() {
        this.invalid = true;
    }

    @Override
    public boolean isValid() {
        return !invalid;
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof BackingCacheEntry)) return false;
        BackingCacheEntry<?, ?> entry = (BackingCacheEntry<?, ?>) object;
        return this.getId().equals(entry.getId());
    }

    @Override
    public int compareTo(BackingCacheEntry<K, V> entry) {
        return Long.valueOf(this.lastUsed).compareTo(entry.getLastUsed());
    }
}