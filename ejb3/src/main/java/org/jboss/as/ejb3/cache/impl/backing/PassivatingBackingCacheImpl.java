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

package org.jboss.as.ejb3.cache.impl.backing;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.ejb.NoSuchEJBException;

import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryFactory;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.spi.GroupCompatibilityChecker;
import org.jboss.as.ejb3.cache.spi.PassivatingBackingCache;
import org.jboss.as.ejb3.cache.spi.BackingCacheLifecycleListener.LifecycleState;
import org.jboss.as.ejb3.cache.spi.ReplicationPassivationManager;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCache;
import org.jboss.as.ejb3.cache.spi.impl.PassivateTask;
import org.jboss.as.ejb3.cache.spi.impl.RemoveTask;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.logging.Logger;

/**
 * @author Paul Ferraro
 *
 */
public class PassivatingBackingCacheImpl<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>> extends AbstractBackingCache<K, V, E> implements PassivatingBackingCache<K, V, E> {
    protected final Logger log = Logger.getLogger(getClass().getName());

    private final StatefulObjectFactory<V> factory;
    private final BackingCacheEntryFactory<K, V, E> entryFactory;
    private final ReplicationPassivationManager<K, E> passivationManager;
    private final BackingCacheEntryStore<K, V, E> store;

    private final ThreadFactory threadFactory;
    private volatile ScheduledExecutorService executor;
    private final Map<K, Future<?>> expirationFutures = new ConcurrentHashMap<K, Future<?>>();
    private final Map<K, Future<?>> passivationFutures = new ConcurrentHashMap<K, Future<?>>();

    public PassivatingBackingCacheImpl(StatefulObjectFactory<V> factory, BackingCacheEntryFactory<K, V, E> entryFactory, ReplicationPassivationManager<K, E> passivationManager, BackingCacheEntryStore<K, V, E> store) {
        this(factory, entryFactory, passivationManager, store, null, null);
    }

    public PassivatingBackingCacheImpl(StatefulObjectFactory<V> factory, BackingCacheEntryFactory<K, V, E> entryFactory, ReplicationPassivationManager<K, E> passivationManager, BackingCacheEntryStore<K, V, E> store, ThreadFactory threadFactory) {
        this(factory, entryFactory, passivationManager, store, threadFactory, null);
    }

    public PassivatingBackingCacheImpl(StatefulObjectFactory<V> factory, BackingCacheEntryFactory<K, V, E> entryFactory, ReplicationPassivationManager<K, E> passivationManager, BackingCacheEntryStore<K, V, E> store, ScheduledExecutorService executor) {
        this(factory, entryFactory, passivationManager, store, null, executor);
    }

    private PassivatingBackingCacheImpl(StatefulObjectFactory<V> factory, BackingCacheEntryFactory<K, V, E> entryFactory, ReplicationPassivationManager<K, E> passivationManager, BackingCacheEntryStore<K, V, E> store, ThreadFactory threadFactory, ScheduledExecutorService executor) {
        this.factory = factory;
        this.entryFactory = entryFactory;
        this.passivationManager = passivationManager;
        this.store = store;
        this.threadFactory = threadFactory;
        this.executor = executor;
    }

    @Override
    public boolean isClustered() {
        return store.isClustered();
    }

    @Override
    public E create() {
        E obj = entryFactory.createEntry(factory.createInstance());
        store.insert(obj);
        return obj;
    }

    @Override
    public E get(K key) throws NoSuchEJBException {
        this.trace("get(%s)", key);

        boolean valid = false;
        while (!valid) {
            E entry = store.get(key);

            if (entry == null) return null;

            entry.lock();
            try {
                valid = entry.isValid();
                if (valid) {
                    if (isClustered()) {
                       passivationManager.postReplicate(entry);
                    }

                    passivationManager.postActivate(entry);

                    entry.setPrePassivated(false);

                    entry.setInUse(true);
                    this.cancelExpirationPassivation(key);
                    return entry;
                }
                // else discard and reacquire
            } finally {
                entry.unlock();
            }
        }
        // Unreachable
        return null;
    }

    @Override
    public void passivate(K key) {
        this.trace("passivate(%s)", key);

        E entry = store.get(key);

        if (entry == null) {
            EjbLogger.ROOT_LOGGER.cacheEntryNotFound(key);
            return;
        }

        // We just *try* to lock; a passivation is low priority.
        if (!entry.tryLock()) {
            throw EjbMessages.MESSAGES.cacheEntryInUse(entry);
        }
        try {
            if (entry.isInUse()) {
                throw EjbMessages.MESSAGES.cacheEntryInUse(entry);
            }

            passivationManager.prePassivate(entry);

            entry.setPrePassivated(true);

            entry.invalidate();

            store.passivate(entry);
        } finally {
            entry.unlock();
        }
    }

    @Override
    public E peek(K key) throws NoSuchEJBException {
        this.trace("peek(%s)", key);

        return store.get(key);
    }

    @Override
    public E release(K key) {
        this.trace("release(%s)", key);

        E entry = store.get(key);
        if (entry == null) {
            EjbLogger.ROOT_LOGGER.cacheEntryNotFound(key);
            return null;
        }
        entry.lock();
        try {
            entry.setInUse(false);

            boolean modified = entry.isModified();
            if (modified) {
                if (isClustered()) {
                    passivationManager.preReplicate(entry);
                }
            }

            store.update(entry, modified);
            this.scheduleExpirationPassivation(key);
            return entry;
        } finally {
            entry.unlock();
        }
    }

    @Override
    public void discard(K key) {
        store.remove(key);
    }

    @Override
    public void remove(K key) {
        this.trace("remove(%s)", key);

        this.cancelExpirationPassivation(key);

        E entry = store.remove(key);

        if (entry == null)
            throw new NoSuchEJBException(String.valueOf(key));

        entry.lock();
        try {
            if (entry.isInUse()) {
                entry.setInUse(false);
            }
            entryFactory.destroyEntry(entry);
            factory.destroyInstance(entry.getUnderlyingItem());
        } finally {
            entry.unlock();
        }
    }

    private void cancelExpirationPassivation(K id) {
        if (this.executor != null) {
            if (this.store.getTimeout() != null) {
                this.cancel(this.expirationFutures, id);
            }
            this.cancel(this.passivationFutures, id);
        }
    }

    private void cancel(Map<K, Future<?>> futures, K id) {
        Future<?> future = futures.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void scheduleExpirationPassivation(K id) {
        if (this.executor != null) {
            StatefulTimeoutInfo timeout = this.store.getTimeout();
            if (timeout != null && timeout.getValue() != -1) {
                this.schedule(this.expirationFutures, id, this.removeTaskFactory, timeout.getValue(), timeout.getTimeUnit());
            }
            BackingCacheEntryStoreConfig config = this.store.getConfig();
            this.schedule(this.passivationFutures, id, this.passivateTaskFactory, config.getIdleTimeout(), config.getIdleTimeoutUnit());
        }
    }

    private void schedule(Map<K, Future<?>> futures, K id, TaskFactory<K> factory, long time, TimeUnit unit) {
        Future<?> future = futures.put(id, this.executor.schedule(factory.createTask(id), time, unit));
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void start() {
        notifyLifecycleListeners(LifecycleState.STARTING);
        try {
            store.start();
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
            store.stop();
            if (this.threadFactory != null) {
                this.executor.shutdownNow();
            }
            notifyLifecycleListeners(LifecycleState.STOPPED);
        } catch (RuntimeException e) {
            notifyLifecycleListeners(LifecycleState.FAILED);
            throw e;
        }
    }

    @Override
    public GroupCompatibilityChecker getCompatibilityChecker() {
        return store;
    }

    interface TaskFactory<K> {
        Runnable createTask(K id);
    }

    private TaskFactory<K> removeTaskFactory = new TaskFactory<K>() {
        @Override
        public Runnable createTask(K id) {
            return new RemoveTask<K>(PassivatingBackingCacheImpl.this, id);
        }
    };

    private TaskFactory<K> passivateTaskFactory = new TaskFactory<K>() {
        @Override
        public Runnable createTask(K id) {
            return new PassivateTask<K>(PassivatingBackingCacheImpl.this, id);
        }
    };

    private void trace(String pattern, Object... args) {
        if (log.isTraceEnabled()) {
            log.tracef(pattern, args);
        }
    }
}
