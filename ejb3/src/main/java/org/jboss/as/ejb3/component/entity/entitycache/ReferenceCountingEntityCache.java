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

    public void create(final EntityBeanComponentInstance instance) {
        if (cache.putIfAbsent(instance.getPrimaryKey(), new CacheEntry(instance)) != null) {
            throw new IllegalArgumentException("Instance for PK [" + instance.getPrimaryKey() + "] already registerd.");
        }
    }

    public EntityBeanComponentInstance get(final Object key) throws NoSuchEntityException {
        if (!cache.containsKey(key)) {
            final EntityBeanComponentInstance instance = createInstance(key);
            create(instance);
        }
        return cache.get(key).instance;
    }

    public void reference(final EntityBeanComponentInstance instance) {
        final CacheEntry cacheEntry = cache.get(instance.getPrimaryKey());
        if (cacheEntry == null) {
            throw new IllegalArgumentException("Instance [" + instance + "] not found in cache");
        }
        cacheEntry.referenceCount.incrementAndGet();
    }

    public void release(final EntityBeanComponentInstance instance, boolean success) {
        if(instance.isDiscarded()) {
            return;
        }
        if(instance.getPrimaryKey() == null) return;  // TODO: Should this be an Exception
        final CacheEntry cacheEntry = cache.get(instance.getPrimaryKey());
        if (cacheEntry == null) {
            throw new IllegalArgumentException("Instance [" + instance + "] not found in cache");
        }
        if (cacheEntry.referenceCount.decrementAndGet() == 0) {
            //TODO: this should probably be somewhere else
            //roll back unsuccessful removal
            if (!success && instance.isRemoved()) {
                instance.setRemoved(false);
            }
            instance.passivate();
            component.getPool().release(instance);
            cache.remove(instance.getPrimaryKey());
        } else if(instance.isRemoved() && success) {
            //the instance has been removed, we need to remove it from the cache
            //even if someone is still referencing it, as their reference is no longer usable
            component.getPool().release(instance);
            cache.remove(instance.getPrimaryKey());
        }
    }

    public void discard(final EntityBeanComponentInstance instance) {
        cache.remove(instance.getPrimaryKey());
    }

    public void start() {
    }

    public void stop() {
    }

    private EntityBeanComponentInstance createInstance(final Object pk) {
        final EntityBeanComponentInstance instance = component.getPool().get();
        instance.associate(pk);
        return instance;
    }

    private class CacheEntry {
        private final AtomicInteger referenceCount = new AtomicInteger(0);
        private final EntityBeanComponentInstance instance;

        private CacheEntry(EntityBeanComponentInstance instance) {
            this.instance = instance;
        }
    }
}
