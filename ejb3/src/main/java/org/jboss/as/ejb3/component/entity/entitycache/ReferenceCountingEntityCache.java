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

package org.jboss.as.ejb3.component.entity.entitycache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.NoSuchEntityException;

import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;

/**
 * @author John Bailey
 */
public class ReferenceCountingEntityCache implements ReadyEntityCache {
    private final ConcurrentMap<Object, CacheEntry> cache = new ConcurrentHashMap<Object, CacheEntry>();
    private final EntityBeanComponent component;

    public ReferenceCountingEntityCache(final EntityBeanComponent component) {
        this.component = component;
    }

    public synchronized void create(final EntityBeanComponentInstance instance) {
        final CacheEntry existing = cache.putIfAbsent(instance.getPrimaryKey(), new CacheEntry(instance));
        if (existing != null) {
            if (existing.instance.isRemoved()) {
                //this happens in an instance is removed and then re-added in the space of the same transaction
                existing.replacedInstance = instance;
            } else {
                throw new IllegalArgumentException("Instance for PK [" + instance.getPrimaryKey() + "] already registerd.");
            }
        }
    }

    public synchronized EntityBeanComponentInstance get(final Object key) throws NoSuchEntityException {
        if (!cache.containsKey(key)) {
            final EntityBeanComponentInstance instance = createInstance(key);
            create(instance);
        }
        final CacheEntry cacheEntry = cache.get(key);
        if (cacheEntry.replacedInstance != null) {
            return cacheEntry.replacedInstance;
        } else {
            return cacheEntry.instance;
        }
    }

    public synchronized void reference(final EntityBeanComponentInstance instance) {
        final CacheEntry cacheEntry = cache.get(instance.getPrimaryKey());
        if (cacheEntry == null) {
            throw new IllegalArgumentException("Instance [" + instance + "] not found in cache");
        }
        cacheEntry.referenceCount.incrementAndGet();
    }

    public synchronized void release(final EntityBeanComponentInstance instance, boolean success) {
        if (instance.isDiscarded()) {
            return;
        }
        if (instance.getPrimaryKey() == null) return;  // TODO: Should this be an Exception
        final CacheEntry cacheEntry = cache.get(instance.getPrimaryKey());
        if (cacheEntry == null) {
            throw new IllegalArgumentException("Instance [" + instance + "] not found in cache");
        }
        if (cacheEntry.replacedInstance != null) {
            //this can happen if an entity is removed and a new entity with the same PK is added in a transactions
            if (instance == cacheEntry.replacedInstance) {
                if (success) {
                    cacheEntry.instance = cacheEntry.replacedInstance;
                } else if (cacheEntry.instance.isDiscarded()) {
                    //if the TX was a failure, and the previous instance has been discarded
                    //we just remove the entry and return
                    cache.remove(instance.getPrimaryKey());
                    return;
                }
                cacheEntry.replacedInstance = null;
            }
        }
        if (cacheEntry.referenceCount.decrementAndGet() == 0) {
            //TODO: this should probably be somewhere else
            //roll back unsuccessful removal
            if (!success && instance.isRemoved()) {
                instance.setRemoved(false);
            }
            final Object pk = instance.getPrimaryKey();
            try {
                instance.passivate();
                component.releaseEntityBeanInstance(instance);
            } finally {
                cache.remove(pk);
            }
        } else if (instance.isRemoved() && success) {
            //the instance has been removed, we need to remove it from the cache
            //even if someone is still referencing it, as their reference is no longer usable
            final Object pk = instance.getPrimaryKey();
            try {
                instance.passivate();
                component.releaseEntityBeanInstance(instance);
            } finally {
                cache.remove(pk);
            }
        }
    }

    public synchronized void discard(final EntityBeanComponentInstance instance) {
        final CacheEntry entry = cache.get(instance.getPrimaryKey());
        if (entry != null) {
            if (instance == entry.replacedInstance) {
                //this instance that is being discarded is the new instance
                //we can just set it to null
                entry.replacedInstance = null;
            } else if (entry.replacedInstance == null) {
                //if there is a new instance we cannot discard the entry entirely
                cache.remove(instance.getPrimaryKey());
            }
        }
    }

    public void start() {
    }

    public void stop() {
    }

    private EntityBeanComponentInstance createInstance(final Object pk) {
        final EntityBeanComponentInstance instance = component.acquireUnAssociatedInstance();
        instance.associate(pk);
        return instance;
    }

    private class CacheEntry {
        private final AtomicInteger referenceCount = new AtomicInteger(0);
        private volatile EntityBeanComponentInstance instance;
        private volatile EntityBeanComponentInstance replacedInstance;

        private CacheEntry(EntityBeanComponentInstance instance) {
            this.instance = instance;
        }
    }
}
