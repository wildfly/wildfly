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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.infinispan.AbstractDelegatingAdvancedCache;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.manager.AbstractDelegatingEmbeddedCacheManager;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author Paul Ferraro
 */
public class DefaultEmbeddedCacheManager extends AbstractDelegatingEmbeddedCacheManager {
    private final String defaultCache;

    public DefaultEmbeddedCacheManager(EmbeddedCacheManager container, String defaultCache) {
        super(container);
        this.defaultCache = defaultCache;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(java.lang.String, org.infinispan.config.Configuration)
     */
    @Override
    public Configuration defineConfiguration(String cacheName, Configuration configurationOverride) {
        return this.cm.defineConfiguration(this.getCacheName(cacheName), configurationOverride);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(java.lang.String, java.lang.String, org.infinispan.config.Configuration)
     */
    @Override
    public Configuration defineConfiguration(String cacheName, String templateCacheName, Configuration configurationOverride) {
        return this.cm.defineConfiguration(this.getCacheName(cacheName), this.getCacheName(templateCacheName), configurationOverride);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(String, org.infinispan.configuration.cache.Configuration)
     */
    @Override
    public org.infinispan.configuration.cache.Configuration defineConfiguration(String cacheName,
            org.infinispan.configuration.cache.Configuration configuration) {
        return this.cm.defineConfiguration(this.getCacheName(cacheName), configuration);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.CacheContainer#getCache()
     */
    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.getCache(this.defaultCache);
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
    public <K, V> Cache<K, V> getCache(String cacheName, boolean start) {
        Cache<K, V> cache = this.cm.<K, V>getCache(this.getCacheName(cacheName), start);
        return (cache != null) ? new DelegatingCache<K, V>(cache) : null;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getCacheNames()
     */
    @Override
    public Set<String> getCacheNames() {
        Set<String> names = new HashSet<String>(this.cm.getCacheNames());
        names.add(this.defaultCache);
        return names;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isDefaultRunning()
     */
    @Override
    public boolean isDefaultRunning() {
        return this.cm.isRunning(this.defaultCache);
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
        Set<String> cacheNames = new LinkedHashSet<String>();
        for (String name: names) {
            cacheNames.add(this.getCacheName(name));
        }
        this.cm.startCaches(cacheNames.toArray(new String[cacheNames.size()]));
        return this;
    }

    private String getCacheName(String name) {
        return ((name == null) || name.equals(CacheContainer.DEFAULT_CACHE_NAME)) ? this.defaultCache : name;
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
        return this.cm.getGlobalConfiguration().getCacheManagerName();
    }

    class DelegatingCache<K, V> extends AbstractAdvancedCache<K, V> {
        DelegatingCache(AdvancedCache<K, V> cache) {
            super(cache);
        }

        DelegatingCache(Cache<K, V> cache) {
            this(cache.getAdvancedCache());
        }

        @Override
        protected AdvancedCache<K, V> wrap(AdvancedCache<K, V> cache) {
            return new DelegatingCache<K, V>(cache);
        }

        @Override
        public EmbeddedCacheManager getCacheManager() {
            return DefaultEmbeddedCacheManager.this;
        }

        @Override
        public AdvancedCache<K, V> with(ClassLoader classLoader) {
            return new ClassLoaderAwareCache<K, V>(this, classLoader);
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
