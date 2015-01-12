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
package org.wildfly.clustering.infinispan.spi.service;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;

/**
 * Service that provides a cache and handles its lifecycle
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class CacheBuilder<K, V> implements Service<Cache<K, V>>, Builder<Cache<K, V>> {

    private static final Logger log = Logger.getLogger(CacheBuilder.class);

    private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
    private final String containerName;
    private final String cacheName;

    private volatile Cache<K, V> cache = null;

    public CacheBuilder(String containerName, String cacheName) {
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheServiceName.CACHE.getServiceName(this.containerName, this.cacheName);
    }

    @Override
    public ServiceBuilder<Cache<K, V>> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(this.getServiceName(), this).build(target)
                .addDependency(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(this.containerName), EmbeddedCacheManager.class, this.container)
                .addDependency(CacheServiceName.CONFIGURATION.getServiceName(this.containerName, this.cacheName))
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Cache<K, V> getValue() {
        return this.cache;
    }

    @Override
    public void start(StartContext context) {
        this.cache = this.container.getValue().getCache(this.cacheName);
        this.cache.start();

        log.debugf("%s %s cache started", this.cacheName, this.containerName);
    }

    @Override
    public void stop(StopContext context) {
        this.cache.stop();
        this.cache = null;

        log.debugf("%s %s cache stopped", this.cacheName, this.containerName);
    }
}