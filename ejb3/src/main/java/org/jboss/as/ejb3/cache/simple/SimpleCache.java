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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.scheduler.LinkedScheduledEntries;
import org.wildfly.clustering.ee.cache.scheduler.LocalScheduler;
import org.wildfly.clustering.ejb.IdentifierFactory;

/**
 * Simple {@link Cache} implementation using in-memory storage and eager expiration.
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @author Paul Ferraro
 */
public class SimpleCache<K, V extends Identifiable<K>> implements Cache<K, V>, Predicate<K> {

    private final ConcurrentMap<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final StatefulObjectFactory<V> factory;
    private final IdentifierFactory<K> identifierFactory;
    private final Duration timeout;
    private final ServerEnvironment environment;
    private final Scheduler<K, Instant> scheduler = new LocalScheduler<>(new LinkedScheduledEntries<>(), this, Duration.ZERO);

    public SimpleCache(StatefulObjectFactory<V> factory, IdentifierFactory<K> identifierFactory, StatefulTimeoutInfo timeout, ServerEnvironment environment) {
        this.factory = factory;
        this.identifierFactory = identifierFactory;

        // A value of -1 means the bean will never be removed due to timeout
        if (timeout == null || timeout.getValue() < 0) {
            this.timeout = null;
        } else {
            this.timeout = Duration.ofMillis(TimeUnit.MILLISECONDS.convert(timeout.getValue(), timeout.getTimeUnit()));
        }

        this.environment = environment;
    }

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        this.scheduler.close();
        for (Map.Entry<K, Entry<V>> entry : this.entries.entrySet()) {
            this.factory.destroyInstance(entry.getValue().getValue());
        }
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
        Entry<V> entry = this.entries.get(key);
        if (entry == null) return null;
        this.scheduler.cancel(key);
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
                if (!this.timeout.isZero()) {
                    this.scheduler.schedule(id, Instant.now().plus(this.timeout));
                } else {
                    // The Jakarta Enterprise Beans specification allows a 0 timeout, which means the bean is immediately eligible for removal.
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

    @Override
    public boolean test(K key) {
        this.remove(key);
        return true;
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
