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

package org.jboss.as.clustering.infinispan;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.impl.AbstractDelegatingEmbeddedCacheManager;
import org.wildfly.clustering.infinispan.spi.CacheContainer;

/**
 * EmbeddedCacheManager decorator that overrides the default cache semantics of a cache manager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainer extends AbstractDelegatingEmbeddedCacheManager implements CacheContainer {

    private final BatcherFactory batcherFactory;

    public DefaultCacheContainer(EmbeddedCacheManager container, BatcherFactory batcherFactory) {
        super(container);
        this.batcherFactory = batcherFactory;
    }

    @Override
    public void start() {
        // No-op.  Lifecycle is managed by container
    }

    @Override
    public void stop() {
        // No-op.  Lifecycle is managed by container
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.wrap(this.cm.<K, V>getCache());
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return this.wrap(this.cm.<K, V>getCache(cacheName));
    }

    @Deprecated
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, String configurationName) {
        return this.wrap(this.cm.<K, V>getCache(cacheName, configurationName));
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
        return this.wrap(this.cm.<K, V>getCache(cacheName, createIfAbsent));
    }

    @Deprecated
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, String configurationTemplate, boolean createIfAbsent) {
        return this.wrap(this.cm.<K, V>getCache(cacheName, configurationTemplate, createIfAbsent));
    }

    private <K, V> Cache<K, V> wrap(Cache<K, V> cache) {
        return new DefaultCache<>(this, this.batcherFactory, cache.getAdvancedCache());
    }

    @Override
    public EmbeddedCacheManager startCaches(String... cacheNames) {
        super.startCaches(cacheNames);
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CacheContainer)) return false;
        CacheContainer container = (CacheContainer) object;
        return this.getName().equals(container.getName());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
