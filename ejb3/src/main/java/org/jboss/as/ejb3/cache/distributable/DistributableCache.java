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
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.clustering.ejb.Batch;
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
public class DistributableCache<K, V extends Identifiable<K>> implements Cache<K, V> {

    private final BeanManager<UUID, K, V> manager;
    private final StatefulObjectFactory<V> factory;
    private final RemoveListener<V> listener;
    private final ServerEnvironment environment;

    public DistributableCache(BeanManager<UUID, K, V> manager, StatefulObjectFactory<V> factory, ServerEnvironment environment) {
        this.manager = manager;
        this.factory = factory;
        this.listener = new RemoveListenerAdapter<>(factory);
        this.environment = environment;
    }

    @Override
    public Affinity getStrictAffinity() {
        Affinity affinity = this.manager.getStrictAffinity();
        return (affinity != null) ? affinity : new NodeAffinity(this.environment.getNodeName());
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
        UUID group = currentGroup.get();
        Batch batch = this.manager.getBatcher().startBatch();
        try {
            if (group == null) {
                newGroup = true;
                group = this.manager.getGroupIdentifierFactory().createIdentifier();
                currentGroup.set(group);
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
                    currentGroup.remove();
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
        BatchStack.pushBatch(this.manager.getBatcher().startBatch());
        try {
            Bean<UUID, K, V> bean = this.manager.findBean(id);
            if (bean == null) {
                BatchStack.popBatch().close();
                return null;
            }
            return bean.acquire();
        } catch (RuntimeException | Error e) {
            BatchStack.popBatch().discard();
            throw e;
        }
    }

    @Override
    public void release(V value) {
        K id = value.getId();
        try {
            Bean<UUID, K, V> bean = this.manager.findBean(id);
            if (bean != null) {
                if (bean.release()) {
                    bean.close();
                }
            }
        } finally {
            BatchStack.popBatch().close();
        }
    }

    @Override
    public void remove(K id) {
        Bean<UUID, K, V> bean = this.manager.findBean(id);
        if (bean != null) {
            bean.remove(this.listener);
        }
        // Batch will be popped in release(...) or discard(...)
    }

    @Override
    public void discard(K id) {
        try {
            Bean<UUID, K, V> bean = this.manager.findBean(id);
            if (bean != null) {
                bean.remove(null);
            }
        } finally {
            BatchStack.popBatch().close();
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
