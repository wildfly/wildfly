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

import java.util.LinkedHashSet;
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.impl.AbstractDelegatingEmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;

/**
 * EmbeddedCacheManager decorator that overrides the default cache semantics of a cache manager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainer extends AbstractDelegatingEmbeddedCacheManager implements CacheContainer {

    private final BatcherFactory batcherFactory;
    private final String defaultCacheName;

    public DefaultCacheContainer(GlobalConfiguration global, String defaultCacheName) {
        this(new DefaultCacheManager(global, null, false), defaultCacheName);
    }

    public DefaultCacheContainer(GlobalConfiguration global, Configuration config, String defaultCacheName) {
        this(new DefaultCacheManager(global, config, false), defaultCacheName);
    }

    public DefaultCacheContainer(EmbeddedCacheManager container, String defaultCacheName) {
        this(container, defaultCacheName, new InfinispanBatcherFactory());
    }

    public DefaultCacheContainer(EmbeddedCacheManager container, String defaultCacheName, BatcherFactory batcherFactory) {
        super(container);
        this.defaultCacheName = defaultCacheName;
        this.batcherFactory = batcherFactory;
    }

    @Override
    public String getDefaultCacheName() {
        return this.defaultCacheName;
    }

    @Override
    public Configuration defineConfiguration(String cacheName, Configuration configuration) {
        return this.cm.defineConfiguration(this.getCacheName(cacheName), configuration);
    }

    @Override
    public Configuration defineConfiguration(String cacheName, String templateCacheName, Configuration configurationOverride) {
        return this.cm.defineConfiguration(this.getCacheName(cacheName), this.getCacheName(templateCacheName), configurationOverride);
    }

    @Override
    public Configuration getDefaultCacheConfiguration() {
        return this.cm.getCacheConfiguration(this.defaultCacheName);
    }

    @Override
    public Configuration getCacheConfiguration(String name) {
        return this.cm.getCacheConfiguration(this.getCacheName(name));
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.CacheContainer#getCache()
     */
    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.getCache(this.defaultCacheName);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.CacheContainer#getCache(java.lang.String)
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return this.getCache(cacheName, true);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getCache(java.lang.String, boolean)
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
        Cache<K, V> cache = this.cm.<K, V>getCache(this.getCacheName(cacheName), createIfAbsent);
        return (cache != null) ? new DelegatingCache<>(this, this.batcherFactory, cache) : null;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isDefaultRunning()
     */
    @Override
    public boolean isDefaultRunning() {
        return this.cm.isRunning(this.defaultCacheName);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isRunning(String)
     */
    @Override
    public boolean isRunning(String cacheName) {
        return this.cm.isRunning(this.getCacheName(cacheName));
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#cacheExists(java.lang.String)
     */
    @Override
    public boolean cacheExists(String cacheName) {
        return this.cm.cacheExists(this.getCacheName(cacheName));
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#removeCache(java.lang.String)
     */
    @Override
    public void removeCache(String cacheName) {
        this.cm.removeCache(this.getCacheName(cacheName));
    }

    @Override
    public EmbeddedCacheManager startCaches(String... names) {
        Set<String> cacheNames = new LinkedHashSet<>();
        for (String name: names) {
            cacheNames.add(this.getCacheName(name));
        }
        this.cm.startCaches(cacheNames.toArray(new String[cacheNames.size()]));
        return this;
    }

    @Override
    public void addCacheDependency(String from, String to) {
        this.cm.addCacheDependency(this.getCacheName(from), this.getCacheName(to));
    }

    private String getCacheName(String name) {
        return ((name == null) || name.equals(CacheServiceNameFactory.DEFAULT_CACHE)) ? this.defaultCacheName : name;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.cm.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName();
    }

    private static class DelegatingCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
        private static final ThreadLocal<Batch> CURRENT_BATCH = new ThreadLocal<>();
        private final EmbeddedCacheManager manager;
        private final Batcher<? extends Batch> batcher;

        DelegatingCache(final EmbeddedCacheManager manager, final BatcherFactory batcherFactory, AdvancedCache<K, V> cache) {
            super(cache, new AdvancedCacheWrapper<K, V>() {
                    @Override
                    public AdvancedCache<K, V> wrap(AdvancedCache<K, V> cache) {
                        return new DelegatingCache<>(manager, batcherFactory, cache);
                    }
                }
            );
            this.manager = manager;
            this.batcher = batcherFactory.createBatcher(cache);
        }

        DelegatingCache(EmbeddedCacheManager manager, BatcherFactory batcherFactory, Cache<K, V> cache) {
            this(manager, batcherFactory, cache.getAdvancedCache());
        }

        @Override
        public EmbeddedCacheManager getCacheManager() {
            return this.manager;
        }

        @Override
        public boolean startBatch() {
            if (this.batcher == null) return false;
            Batch batch = CURRENT_BATCH.get();
            if (batch != null) return false;
            CURRENT_BATCH.set(this.batcher.createBatch());
            return true;
        }

        @Override
        public void endBatch(boolean successful) {
            Batch batch = CURRENT_BATCH.get();
            if (batch != null) {
                try {
                    if (successful) {
                        batch.close();
                    } else {
                        batch.discard();
                    }
                } finally {
                    CURRENT_BATCH.remove();
                }
            }
        }

        // Workaround for https://hibernate.atlassian.net/browse/HHH-9337
        @Override
        public void removeListener(Object listener) {
            if (listener.getClass().isAnnotationPresent(Listener.class)) {
                super.removeListener(listener);
            }
        }

        @Override
        public boolean equals(Object object) {
            return (object == this) || (object == this.cache);
        }

        @Override
        public int hashCode() {
            return this.cache.hashCode();
        }
    }
}
