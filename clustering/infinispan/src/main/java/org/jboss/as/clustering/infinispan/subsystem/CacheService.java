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

package org.jboss.as.clustering.infinispan.subsystem;

import javax.transaction.xa.XAResource;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.AsynchronousService;
import org.jboss.msc.service.ServiceName;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheService<K, V> extends AsynchronousService<Cache<K, V>> {

    private final Dependencies dependencies;
    private final String name;

    private volatile Cache<K, V> cache;
    private volatile XAResourceRecovery recovery;

    public static ServiceName getServiceName(String container, String cache) {
        return EmbeddedCacheManagerService.getServiceName(container).append((cache != null) ? cache : CacheContainer.DEFAULT_CACHE_NAME);
    }

    interface Dependencies {
        EmbeddedCacheManager getCacheContainer();
        XAResourceRecoveryRegistry getRecoveryRegistry();
    }

    public CacheService(String name, Dependencies dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public Cache<K, V> getValue() throws IllegalStateException, IllegalArgumentException {
        return this.cache;
    }

    @Override
    protected void start() {
        EmbeddedCacheManager container = this.dependencies.getCacheContainer();

        this.cache = container.getCache(this.name);
        this.cache.start();

        XAResourceRecoveryRegistry recoveryRegistry = this.dependencies.getRecoveryRegistry();
        if (recoveryRegistry != null) {
            this.recovery = new InfinispanXAResourceRecovery(this.name, container);
            recoveryRegistry.addXAResourceRecovery(this.recovery);
        }
    }

    @Override
    protected void stop() {
        if ((this.cache != null) && this.cache.getStatus().allowInvocations()) {
            if (this.recovery != null) {
                this.dependencies.getRecoveryRegistry().removeXAResourceRecovery(this.recovery);
            }

            this.cache.stop();
        }
    }

    static class InfinispanXAResourceRecovery implements XAResourceRecovery {
        private final String cacheName;
        private final EmbeddedCacheManager container;

        InfinispanXAResourceRecovery(String cacheName, EmbeddedCacheManager container) {
            this.cacheName = cacheName;
            this.container = container;
        }

        @Override
        public XAResource[] getXAResources() {
            return new XAResource[] { this.container.getCache(this.cacheName).getAdvancedCache().getXAResource() };
        }

        @Override
        public int hashCode() {
            return this.container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName().hashCode() ^ this.cacheName.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if ((object == null) || !(object instanceof InfinispanXAResourceRecovery)) return false;
            InfinispanXAResourceRecovery recovery = (InfinispanXAResourceRecovery) object;
            return this.container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName().equals(recovery.container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName()) && this.cacheName.equals(recovery.cacheName);
        }

        @Override
        public String toString() {
            return container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName() + "." + this.cacheName;
        }
    }
}
