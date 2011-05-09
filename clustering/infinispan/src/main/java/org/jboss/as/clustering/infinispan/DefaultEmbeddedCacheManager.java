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

import java.security.AccessController;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.infinispan.AbstractDelegatingAdvancedCache;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.stats.Stats;
import org.jboss.util.loading.ContextClassLoaderSwitcher;
import org.jboss.util.loading.ContextClassLoaderSwitcher.SwitchContext;

/**
 * @author Paul Ferraro
 */
public class DefaultEmbeddedCacheManager implements EmbeddedCacheManager {
    @SuppressWarnings("unchecked")
    private static final ContextClassLoaderSwitcher switcher = (ContextClassLoaderSwitcher) AccessController.doPrivileged(ContextClassLoaderSwitcher.INSTANTIATOR);

    private final String defaultCache;
    private final EmbeddedCacheManager container;

    public DefaultEmbeddedCacheManager(EmbeddedCacheManager container, String defaultCache) {
        this.container = container;
        this.defaultCache = defaultCache;
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
        SwitchContext context = start ? switcher.getSwitchContext(DefaultEmbeddedCacheManager.class.getClassLoader()) : null;
        try {
            Cache<K, V> cache = this.container.<K, V>getCache(this.getCacheName(cacheName), start);
            return (cache != null) ? new DelegatingCache<K, V>(cache) : null;
        } finally {
            if (context != null) {
                context.reset();
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.lifecycle.Lifecycle#start()
     */
    @Override
    public void start() {
        this.container.start();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.lifecycle.Lifecycle#stop()
     */
    @Override
    public void stop() {
        this.container.stop();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.notifications.Listenable#addListener(java.lang.Object)
     */
    @Override
    public void addListener(Object listener) {
        this.container.addListener(listener);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.notifications.Listenable#removeListener(java.lang.Object)
     */
    @Override
    public void removeListener(Object listener) {
        this.container.removeListener(listener);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.notifications.Listenable#getListeners()
     */
    @Override
    public Set<Object> getListeners() {
        return this.container.getListeners();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(java.lang.String, org.infinispan.config.Configuration)
     */
    @Override
    public Configuration defineConfiguration(String cacheName, Configuration configurationOverride) {
        return this.container.defineConfiguration(this.getCacheName(cacheName), configurationOverride);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#defineConfiguration(java.lang.String, java.lang.String, org.infinispan.config.Configuration)
     */
    @Override
    public Configuration defineConfiguration(String cacheName, String templateCacheName, Configuration configurationOverride) {
        return this.container.defineConfiguration(this.getCacheName(cacheName), templateCacheName, configurationOverride);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getClusterName()
     */
    @Override
    public String getClusterName() {
        return this.container.getClusterName();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getMembers()
     */
    @Override
    public List<Address> getMembers() {
        return this.container.getMembers();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getAddress()
     */
    @Override
    public Address getAddress() {
        return this.container.getAddress();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getCoordinator()
     */
    @Override
    public Address getCoordinator() {
        return this.container.getCoordinator();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isCoordinator()
     */
    @Override
    public boolean isCoordinator() {
        return this.container.isCoordinator();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getStatus()
     */
    @Override
    public ComponentStatus getStatus() {
        return this.container.getStatus();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getGlobalConfiguration()
     */
    @Override
    public GlobalConfiguration getGlobalConfiguration() {
        return this.container.getGlobalConfiguration();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getDefaultConfiguration()
     */
    @Override
    public Configuration getDefaultConfiguration() {
        return this.container.getDefaultConfiguration();
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#getCacheNames()
     */
    @Override
    public Set<String> getCacheNames() {
        Set<String> names = new HashSet<String>(this.container.getCacheNames());
        names.add(this.defaultCache);
        return names;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isRunning(java.lang.String)
     */
    @Override
    public boolean isRunning(String cacheName) {
        return this.container.isRunning(this.getCacheName(cacheName));
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#isDefaultRunning()
     */
    @Override
    public boolean isDefaultRunning() {
        return this.container.isRunning(this.defaultCache);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#cacheExists(java.lang.String)
     */
    @Override
    public boolean cacheExists(String cacheName) {
        return this.container.cacheExists(this.getCacheName(cacheName));
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.manager.EmbeddedCacheManager#removeCache(java.lang.String)
     */
    @Override
    public void removeCache(String cacheName) {
        this.container.removeCache(this.getCacheName(cacheName));
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
        return this.container.getGlobalConfiguration().getCacheManagerName();
    }

    class DelegatingCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
        private final AdvancedCache<K, V> cache;

        DelegatingCache(AdvancedCache<K, V> cache) {
            super(cache);
            this.cache = cache;
        }

        DelegatingCache(Cache<K, V> cache) {
            this(cache.getAdvancedCache());
        }

        @Override
        public EmbeddedCacheManager getCacheManager() {
            return DefaultEmbeddedCacheManager.this;
        }

        @Override
        public AdvancedCache<K, V> getAdvancedCache() {
            return this;
        }

        @Override
        public Stats getStats() {
            return this.cache.getStats();
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
