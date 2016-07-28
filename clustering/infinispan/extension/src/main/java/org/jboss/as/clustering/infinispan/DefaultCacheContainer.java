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

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.manager.impl.AbstractDelegatingEmbeddedCacheManager;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.service.SubGroupServiceNameFactory;

/**
 * EmbeddedCacheManager decorator that overrides the default cache semantics of a cache manager.
 * @author Paul Ferraro
 */
public class DefaultCacheContainer extends AbstractDelegatingEmbeddedCacheManager implements CacheContainer {

    private final String name;
    private final BatcherFactory batcherFactory;
    private final String defaultCacheName;

    public DefaultCacheContainer(String name, EmbeddedCacheManager container, String defaultCacheName) {
        this(name, container, defaultCacheName, new InfinispanBatcherFactory());
    }

    public DefaultCacheContainer(String name, EmbeddedCacheManager container, String defaultCacheName, BatcherFactory batcherFactory) {
        super(container);
        this.name = name;
        this.defaultCacheName = defaultCacheName;
        this.batcherFactory = batcherFactory;
    }

    @Override
    public String getName() {
        return this.name;
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
    public void undefineConfiguration(String cacheName) {
        this.cm.undefineConfiguration(this.getCacheName(cacheName));
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

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return this.getCache(cacheName, cacheName);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, String configurationName) {
        Cache<K, V> cache = this.cm.<K, V>getCache(this.getCacheName(cacheName), this.getCacheName(configurationName));
        return (cache != null) ? new DefaultCache<>(this, this.batcherFactory, cache.getAdvancedCache()) : null;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, boolean createIfAbsent) {
        return this.getCache(cacheName, cacheName, createIfAbsent);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, String configurationTemplate, boolean createIfAbsent) {
        boolean cacheExists = this.cacheExists(cacheName);
        return (cacheExists || createIfAbsent) ? this.getCache(cacheName, configurationTemplate) : null;
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
        return ((name == null) || name.equals(SubGroupServiceNameFactory.DEFAULT_SUB_GROUP)) ? this.defaultCacheName : name;
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
}
