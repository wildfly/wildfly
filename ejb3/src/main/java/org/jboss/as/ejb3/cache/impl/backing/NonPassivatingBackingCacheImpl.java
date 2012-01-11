/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.ejb.NoSuchEJBException;

import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.cache.spi.BackingCache;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryFactory;
import org.jboss.as.ejb3.cache.spi.BackingCacheLifecycleListener.LifecycleState;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCache;
import org.jboss.as.ejb3.cache.spi.impl.RemoveTask;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;

/**
 * Simple {@link BackingCache} that doesn't handle passivation (although it does handle expiration). Pure in-VM memory cache.
 * Not group-aware, as there is no point in managing groups if there is no serialization.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class NonPassivatingBackingCacheImpl<K extends Serializable, V extends Cacheable<K>> extends AbstractBackingCache<K, V, NonPassivatingBackingCacheEntry<K, V>> implements BackingCacheEntryFactory<K, V, NonPassivatingBackingCacheEntry<K, V>> {
    private final StatefulObjectFactory<V> factory;
    private final Map<K, NonPassivatingBackingCacheEntry<K, V>> cache = new ConcurrentHashMap<K, NonPassivatingBackingCacheEntry<K, V>>();
    private final StatefulTimeoutInfo timeout;
    private volatile ScheduledExecutorService executor;
    private final ThreadFactory threadFactory;
    private final Map<K, Future<?>> expirationFutures = new ConcurrentHashMap<K, Future<?>>();
    private final ServerEnvironment environment;

    public NonPassivatingBackingCacheImpl(StatefulObjectFactory<V> factory, ThreadFactory threadFactory, StatefulTimeoutInfo timeout, ServerEnvironment environment) {
        this.factory = factory;
        this.timeout = timeout;
        this.threadFactory = threadFactory;
        this.environment = environment;
    }

    public NonPassivatingBackingCacheImpl(StatefulObjectFactory<V> factory, ScheduledExecutorService executor, StatefulTimeoutInfo timeout, ServerEnvironment environment) {
        this.factory = factory;
        this.timeout = timeout;
        this.executor = executor;
        this.threadFactory = null;
        this.environment = environment;
    }

    @Override
    public Affinity getStrictAffinity() {
        return new NodeAffinity(this.environment.getNodeName());
    }

    @Override
    public Affinity getWeakAffinity(K key) {
        return Affinity.NONE;
    }

    @Override
    public boolean hasAffinity(K key) {
        return true;
    }

    @Override
    public NonPassivatingBackingCacheEntry<K, V> create() {
        NonPassivatingBackingCacheEntry<K, V> entry = this.createEntry(this.factory.createInstance());
        cache.put(entry.getUnderlyingItem().getId(), entry);
        return entry;
    }

    @Override
    public NonPassivatingBackingCacheEntry<K, V> createEntry(V item) {
        return new NonPassivatingBackingCacheEntry<K, V>(item);
    }

    @Override
    public void destroyEntry(NonPassivatingBackingCacheEntry<K, V> entry) {
        this.factory.destroyInstance(entry.getUnderlyingItem());
    }

    @Override
    public NonPassivatingBackingCacheEntry<K, V> get(K key) throws NoSuchEJBException {
        NonPassivatingBackingCacheEntry<K, V> entry = cache.get(key);
        if (entry == null) return null;
        entry.setInUse(true);
        this.scheduleExpiration(key, true);
        return entry;
    }

    @Override
    public NonPassivatingBackingCacheEntry<K, V> peek(K key) throws NoSuchEJBException {
        return cache.get(key);
    }

    @Override
    public NonPassivatingBackingCacheEntry<K, V> release(K key) {
        NonPassivatingBackingCacheEntry<K, V> entry = cache.get(key);
        if (entry == null) {
            EjbLogger.ROOT_LOGGER.cacheEntryNotFound(key);
            return null;
        }
        if (!entry.isInUse()) {
            throw EjbMessages.MESSAGES.cacheEntryNotInUse(key);
        }
        entry.setInUse(false);
        this.scheduleExpiration(key, false);
        return entry;
    }

    @Override
    public void discard(K key) {
        cache.remove(key);
    }

    @Override
    public void remove(K key) {
        this.scheduleExpiration(key, true);
        NonPassivatingBackingCacheEntry<K, V> entry = cache.remove(key);
        if (entry != null && entry.isInUse()) {
            entry.setInUse(false);
        }
        if (entry != null) {
            factory.destroyInstance(entry.getUnderlyingItem());
        }
    }

    @Override
    public boolean isClustered() {
        return false;
    }

    @Override
    public void start() {
        notifyLifecycleListeners(LifecycleState.STARTING);
        try {
            if (this.threadFactory != null) {
                this.executor = Executors.newScheduledThreadPool(1, this.threadFactory);
            }
            notifyLifecycleListeners(LifecycleState.STARTED);
        } catch (RuntimeException e) {
            notifyLifecycleListeners(LifecycleState.FAILED);
            throw e;
        }
    }

    @Override
    public void stop() {
        notifyLifecycleListeners(LifecycleState.STOPPING);
        try {
            if (this.threadFactory != null) {
                this.executor.shutdownNow();
            } else {
                // This is a shared executor, so just cancel our tasks
                for (Future<?> future: this.expirationFutures.values()) {
                    future.cancel(false);
                }
            }
            this.expirationFutures.clear();
            this.cache.clear();
            notifyLifecycleListeners(LifecycleState.STOPPED);
        } catch (RuntimeException e) {
            notifyLifecycleListeners(LifecycleState.FAILED);
            throw e;
        }
    }

    private void scheduleExpiration(K id, boolean cancel) {
        if (this.timeout != null && timeout.getValue() != -1) {
            Future<?> future = cancel ? this.expirationFutures.remove(id) : this.expirationFutures.put(id, this.executor.schedule(new RemoveTask<K>(this, id), this.timeout.getValue(), this.timeout.getTimeUnit()));
            if (future != null) {
                future.cancel(false);
            }
        }
    }
}
