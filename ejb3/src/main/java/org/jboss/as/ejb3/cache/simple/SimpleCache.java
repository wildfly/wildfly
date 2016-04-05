/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.cache.simple;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.ejb.IdentifierFactory;

/**
 * Simple {@link Cache} implementation using in-memory storage and eager expiration.
 *
 * @author Paul Ferraro
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class SimpleCache<K, V extends Identifiable<K>> implements Cache<K, V> {

    final Map<K, Future<?>> expirationFutures = new ConcurrentHashMap<>();
    private final ConcurrentMap<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final StatefulObjectFactory<V> factory;
    private final IdentifierFactory<K> identifierFactory;
    private final StatefulTimeoutInfo timeout;
    private final ServerEnvironment environment;
    private final ScheduledExecutorService executor;

    public SimpleCache(StatefulObjectFactory<V> factory, IdentifierFactory<K> identifierFactory, StatefulTimeoutInfo timeout, ServerEnvironment environment, ScheduledExecutorService executor) {
        this.factory = factory;
        this.identifierFactory = identifierFactory;
        this.timeout = timeout;
        this.environment = environment;
        this.executor = executor;
    }

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        for (Future<?> future: this.expirationFutures.values()) {
            future.cancel(true);
        }
        for (Future<?> future: this.expirationFutures.values()) {
            if (!future.isCancelled() && !future.isDone()) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    // Ignore
                }
            }
        }
        for(Map.Entry<K, Entry<V>> entry : entries.entrySet()) {
            this.factory.destroyInstance(entry.getValue().getValue());
        }
        this.expirationFutures.clear();
        this.entries.clear();
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
    public K createIdentifier() {
        return this.identifierFactory.createIdentifier();
    }

    @Override
    public V create() {
        if (CURRENT_GROUP.get() != null) {
            // An SFSB that uses a distributable cache cannot contain an SFSB that uses a simple cache
            throw EjbLogger.ROOT_LOGGER.incompatibleCaches();
        }
        V bean = this.factory.createInstance();
        this.entries.put(bean.getId(), new Entry<>(bean));
        return bean;
    }

    @Override
    public void discard(V value) {
        this.entries.remove(value.getId());
    }

    @Override
    public void remove(K key) {
        Entry<V> entry = this.entries.remove(key);
        if (entry != null) {
            this.factory.destroyInstance(entry.getValue());
        }
    }

    @Override
    public V get(K key) {
        Future<?> future = this.expirationFutures.get(key);
        if (future != null) {
            future.cancel(true);
        }
        Entry<V> entry = this.entries.get(key);
        if (entry == null) return null;
        entry.use();
        return entry.getValue();
    }

    @Override
    public boolean contains(K key) {
        return this.entries.containsKey(key);
    }

    @Override
    public void release(V bean) {
        K id = bean.getId();
        Entry<V> entry = this.entries.get(id);
        if ((entry != null) && entry.done()) {
            if (this.timeout != null) {
                long value = this.timeout.getValue();
                if (value > 0) {
                    TimeUnit unit = this.timeout.getTimeUnit();
                    RemoveTask task = new RemoveTask(id);
                    // Make sure the expiration future map insertion happens before map removal (during task execution).
                    synchronized (task) {
                        this.expirationFutures.put(id, this.executor.schedule(task, value, unit));
                    }
                } else if (value == 0) {
                    // The EJB specification allows a 0 timeout, which means the bean is immediately eligible for removal.
                    // However, removing it directly is faster than scheduling it for immediate removal.
                    remove(id);
                }
            }
        }
    }

    @Override
    public int getCacheSize() {
        return this.entries.size();
    }

    @Override
    public int getPassivatedCount() {
        return 0;
    }

    @Override
    public int getTotalSize() {
        return this.getCacheSize();
    }

    class RemoveTask implements Runnable {
        private final K key;

        RemoveTask(K key) {
            this.key = key;
        }

        @Override
        public synchronized void run() {
            if (!Thread.currentThread().isInterrupted()) {
                SimpleCache.this.remove(this.key);
            }
            SimpleCache.this.expirationFutures.remove(this.key);
        }
    }

    static class Entry<V> {
        private final V value;
        private final AtomicInteger usage = new AtomicInteger();

        Entry(V value) {
            this.value = value;
        }

        void use() {
            this.usage.incrementAndGet();
        }

        boolean done() {
            return this.usage.decrementAndGet() == 0;
        }

        V getValue() {
            return this.value;
        }
    }
}
