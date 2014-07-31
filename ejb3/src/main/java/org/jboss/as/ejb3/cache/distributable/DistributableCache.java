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

import java.util.UUID;

import org.jboss.as.ejb3.cache.Cache;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.ejb.client.Affinity;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.BatchContext;
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

    private final BeanManager<UUID, K, V, Batch> manager;
    private final StatefulObjectFactory<V> factory;
    private final RemoveListener<V> listener;

    public DistributableCache(BeanManager<UUID, K, V, Batch> manager, StatefulObjectFactory<V> factory) {
        this.manager = manager;
        this.factory = factory;
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

    @Override
    public K createIdentifier() {
        return this.manager.getBeanIdentifierFactory().createIdentifier();
    }

    @Override
    public V create() {
        boolean newGroup = false;
        boolean success = false;
        UUID group = CURRENT_GROUP.get();
        Batch batch = this.manager.getBatcher().startBatch();
        try {
            if (group == null) {
                newGroup = true;
                group = this.manager.getGroupIdentifierFactory().createIdentifier();
                CURRENT_GROUP.set(group);
            }

            try {
                // This will invoke Cache.create() for nested beans
                // Nested beans will share the same group identifier
                V instance = this.factory.createInstance();
                K id = instance.getId();
                this.manager.createBean(id, group, instance).close();
                success = true;
                return instance;
            } finally {
                if (newGroup) {
                    CURRENT_GROUP.remove();
                }
            }
        } finally {
            if (success) {
                batch.close();
            } else {
                batch.discard();
            }
        }
    }

    @Override
    public V get(K id) {
        Batch batch = this.manager.getBatcher().startBatch();
        try {
            Bean<UUID, K, V> bean = this.manager.findBean(id);
            if (bean == null) {
                batch.close();
                return null;
            }
            V result = bean.acquire();
            result.setCacheContext(batch);
            return result;
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        }
    }

    @Override
    public void release(V value) {
        try (BatchContext context = this.manager.getBatcher().resume(value.getCacheContext())) {
            try (Batch batch = value.getCacheContext()) {
                Bean<UUID, K, V> bean = this.manager.findBean(value.getId());
                if (bean != null) {
                    if (bean.release()) {
                        bean.close();
                    }
                }
            }
        }
    }

    @Override
    public void remove(K id) {
        Bean<UUID, K, V> bean = this.manager.findBean(id);
        if (bean != null) {
            bean.remove(this.listener);
        }
    }

    @Override
    public void discard(V value) {
        try (BatchContext context = this.manager.getBatcher().resume(value.getCacheContext())) {
            try (Batch batch = value.getCacheContext()) {
                Bean<UUID, K, V> bean = this.manager.findBean(value.getId());
                if (bean != null) {
                    bean.remove(null);
                }
            }
        }
    }

    @Override
    public boolean contains(K id) {
        return this.manager.containsBean(id);
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
}
