/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.NoSuchEJBException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;

/**
 * Cache of entity bean component instances by transaction key
 *
 * @author Stuart Douglas
 */
public class TransactionLocalEntityCache implements ReadyEntityCache {


    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final ConcurrentMap<Object, Map<Object, CacheEntry>> cache = new ConcurrentHashMap<Object, Map<Object, CacheEntry>>(Runtime.getRuntime().availableProcessors());
    private final EntityBeanComponent component;

    public TransactionLocalEntityCache(final EntityBeanComponent component) {
        this.component = component;
        this.transactionSynchronizationRegistry = component.getTransactionSynchronizationRegistry();
    }

    @Override
    public EntityBeanComponentInstance get(final Object key) throws NoSuchEJBException {
        if (!isTransactionActive()) {
            return createInstance(key);
        }

        final Map<Object, CacheEntry> cache = prepareCache();
        if (!cache.containsKey(key)) {
            final EntityBeanComponentInstance instance = createInstance(key);
            create(instance);
        }
        return cache.get(key).instance;
    }

    @Override
    public void discard(final EntityBeanComponentInstance instance) {
        if (isTransactionActive()) {
            final Object key = transactionSynchronizationRegistry.getTransactionKey();
            final Map<Object, CacheEntry> map = cache.get(key);
            if (map != null) {
                map.remove(instance.getPrimaryKey());
            }
        }
    }

    @Override
    public void create(final EntityBeanComponentInstance instance) throws NoSuchEJBException {
        if (isTransactionActive()) {
            final Map<Object, CacheEntry> map = prepareCache();
            map.put(instance.getPrimaryKey(), new CacheEntry(instance));
        }
    }

    @Override
    public void release(final EntityBeanComponentInstance instance, boolean success) {

        if (instance.isDiscarded()) {
            return;
        }
        if (instance.getPrimaryKey() == null) {
            return;
        }
        final Object key = transactionSynchronizationRegistry.getTransactionKey();
        if (key == null) {
            return;
        }
        final Map<Object, CacheEntry> map = cache.get(key);
        if (map != null) {
            final CacheEntry cacheEntry = map.get(instance.getPrimaryKey());
            if (cacheEntry == null) {
                throw new IllegalArgumentException("Instance [" + instance + "] not found in cache");
            }
            if (cacheEntry.referenceCount.decrementAndGet() <= 0) {
                if (!success && instance.isRemoved()) {
                    instance.setRemoved(false);
                }
                final Object pk = instance.getPrimaryKey();
                try {
                    instance.passivate();
                    component.releaseEntityBeanInstance(instance);
                } finally {
                    map.remove(pk);
                }
            } else if (instance.isRemoved() && success) {
                //the instance has been removed, we need to remove it from the cache
                //even if someone is still referencing it, as their reference is no longer usable
                final Object pk = instance.getPrimaryKey();
                try {
                    instance.passivate();
                    component.releaseEntityBeanInstance(instance);
                } finally {
                    map.remove(pk);
                }
            }
        }
    }

    public void reference(EntityBeanComponentInstance instance) {
        final Map<Object, CacheEntry> cache = prepareCache();
        final CacheEntry cacheEntry = cache.get(instance.getPrimaryKey());
        if (cacheEntry == null) {
            throw new IllegalArgumentException("Instance [" + instance + "] not found in cache");
        }
        cacheEntry.referenceCount.incrementAndGet();
    }

    @Override
    public synchronized void start() {

    }

    @Override
    public synchronized void stop() {
    }

    private Map<Object, CacheEntry> prepareCache() {
        final Object key = transactionSynchronizationRegistry.getTransactionKey();
        Map<Object, CacheEntry> map = cache.get(key);
        if (map != null) {
            return map;
        }
        map = Collections.synchronizedMap(new HashMap<Object, CacheEntry>());
        final Map<Object, CacheEntry> existing = cache.putIfAbsent(key, map);
        if (existing != null) {
            map = existing;
        }
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {

            }

            @Override
            public void afterCompletion(final int status) {
                cache.remove(key);
            }
        });
        return map;
    }

    private EntityBeanComponentInstance createInstance(Object pk) {
        final EntityBeanComponentInstance instance = component.acquireUnAssociatedInstance();
        instance.associate(pk);
        return instance;
    }

    private boolean isTransactionActive() {
        return transactionSynchronizationRegistry.getTransactionKey() != null;
    }

    private class CacheEntry {
        private final AtomicInteger referenceCount = new AtomicInteger(0);
        private final EntityBeanComponentInstance instance;

        private CacheEntry(EntityBeanComponentInstance instance) {
            this.instance = instance;
        }
    }
}
