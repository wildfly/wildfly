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
package org.jboss.as.ejb3.cache.distributable;

import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.ejb.client.Affinity;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.BeanManager;
import org.wildfly.clustering.ejb.RemoveListener;

/**
 * Distributable {@link Cache} implementation.
 * This object is responsible for:
 * <ul>
 *     <li>Group association based on creation context</li>
 *     <li>Batching of bean manager operations</li>
 * </ul>
 * @author Paul Ferraro
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCache<K, V extends Identifiable<K> & Contextual<Batch>> implements Cache<K, V> {
    private final BeanManager<K, V, Batch> manager;
    private final StatefulObjectFactory<V> factory;
    private final TransactionSynchronizationRegistry tsr;
    private final RemoveListener<V> listener;

    public DistributableCache(BeanManager<K, V, Batch> manager, StatefulObjectFactory<V> factory, TransactionSynchronizationRegistry tsr) {
        this.manager = manager;
        this.factory = factory;
        this.tsr = tsr;
        this.listener = new RemoveListenerAdapter<>(factory);
    }

    @Override
    public Affinity getStrictAffinity() {
        return this.manager.getStrictAffinity();
    }

    @Override
    public Affinity getWeakAffinity(K id) {
        return this.manager.getWeakAffinity(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public K createIdentifier() {
        K id = this.manager.getIdentifierFactory().createIdentifier();
        K group = (K) CURRENT_GROUP.get();
        if (group == null) {
            group = id;
            CURRENT_GROUP.set(group);
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V create() {
        boolean newGroup = CURRENT_GROUP.get() == null;
        try (Batch batch = this.manager.getBatcher().createBatch()) {
            try {
                // This will invoke Cache.create() for nested beans
                // Nested beans will share the same group identifier
                V instance = this.factory.createInstance();
                K id = instance.getId();
                this.manager.createBean(id, (K) CURRENT_GROUP.get(), instance).close();
                return instance;
            } catch (RuntimeException | Error e) {
                batch.discard();
                throw e;
            }
        } finally {
            if (newGroup) {
                CURRENT_GROUP.remove();
            }
        }
    }

    @SuppressWarnings("resource")
    @Override
    public V get(K id) {
        Batcher<Batch> batcher = this.manager.getBatcher();
        boolean transactional = (this.tsr.getTransactionKey() != null);
        // Batch may already be associated with this tx
        Batch existingBatch = transactional ? (Batch) this.tsr.getResource(Batch.class) : null;
        try (BatchContext context = (existingBatch != null) ? batcher.resumeBatch(existingBatch) : null) {
            // Batch is not closed here - it will be closed during release(...) or discard(...)
            Batch batch = batcher.createBatch();
            try {
                Bean<K, V> bean = this.manager.findBean(id);
                if (bean == null) {
                    batch.close();
                    return null;
                }
                V result = bean.acquire();
                result.setCacheContext(batch);
                if (transactional && (existingBatch == null)) {
                    // Leverage TSR to propagate Batch reference across calls to Cache.get(...) by different threads for the same tx
                    this.tsr.putResource(Batch.class, batch);
                }
                return result;
            } catch (RuntimeException | Error e) {
                batch.discard();
                batch.close();
                throw e;
            }
        }
    }

    @Override
    public void release(V value) {
        try (BatchContext context = this.manager.getBatcher().resumeBatch(value.getCacheContext())) {
            try (Batch batch = value.getCacheContext()) {
                try {
                    Bean<K, V> bean = this.manager.findBean(value.getId());
                    if (bean != null) {
                        if (bean.release()) {
                            bean.close();
                        }
                    }
                } catch (RuntimeException | Error e) {
                    batch.discard();
                    throw e;
                }
            }
        }
    }

    @Override
    public void remove(K id) {
        try (Batch batch = this.manager.getBatcher().createBatch()) {
            try {
                Bean<K, V> bean = this.manager.findBean(id);
                if (bean != null) {
                    bean.remove(this.listener);
                }
            } catch (RuntimeException | Error e) {
                batch.discard();
                throw e;
            }
        }
    }

    @Override
    public void discard(V value) {
        try (BatchContext context = this.manager.getBatcher().resumeBatch(value.getCacheContext())) {
            try (Batch batch = value.getCacheContext()) {
                try {
                    Bean<K, V> bean = this.manager.findBean(value.getId());
                    if (bean != null) {
                        bean.remove(null);
                    }
                } catch (RuntimeException | Error e) {
                    batch.discard();
                    throw e;
                }
            }
        }
    }

    @Override
    public boolean contains(K id) {
        try (Batch batch = this.manager.getBatcher().createBatch()) {
            return this.manager.containsBean(id);
        }
    }

    @Override
    public void start() {
        this.manager.start();
    }

    @Override
    public void stop() {
        this.manager.stop();
    }

    @Override
    public int getCacheSize() {
        return this.manager.getActiveCount();
    }

    @Override
    public int getPassivatedCount() {
        return this.manager.getPassiveCount();
    }

    @Override
    public int getTotalSize() {
        return this.manager.getActiveCount() + this.manager.getPassiveCount();
    }

    @Override
    public boolean isRemotable(Throwable throwable) {
        return this.manager.isRemotable(throwable);
    }
}
