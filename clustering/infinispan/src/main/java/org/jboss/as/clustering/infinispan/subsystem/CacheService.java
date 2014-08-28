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
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.InterceptorChain;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionCoordinator;
import org.infinispan.transaction.TransactionTable;
import org.infinispan.transaction.xa.recovery.RecoveryManager;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.TxInterceptor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheService<K, V> implements Service<Cache<K, V>> {

    private final Dependencies dependencies;
    private final String name;

    private volatile Cache<K, V> cache;
    private volatile XAResourceRecovery recovery;

    private static final Logger log = Logger.getLogger(CacheService.class.getPackage().getName());
    public static ServiceName getServiceName(String container, String cache) {
        return EmbeddedCacheManagerService.getServiceName(container).append((cache != null) ? cache : CacheContainer.DEFAULT_CACHE_ALIAS);
    }

    public interface Dependencies {
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
    public Cache<K, V> getValue() {
        return this.cache;
    }

    @Override
    public void start(StartContext context) {
        EmbeddedCacheManager container = this.dependencies.getCacheContainer();

        this.cache = container.getCache(this.name);
        this.cache.start();

        // Temporary workaround for ISPN-4196
        Configuration config = this.cache.getCacheConfiguration();
        if (config.transaction().transactionMode().isTransactional()) {
            ComponentRegistry registry = this.cache.getAdvancedCache().getComponentRegistry();
            InterceptorChain chain = registry.getComponent(InterceptorChain.class);
            TransactionTable table = registry.getComponent(TransactionTable.class);
            TransactionCoordinator coordinator = registry.getComponent(TransactionCoordinator.class);
            RecoveryManager recovery = registry.getComponent(RecoveryManager.class);
            TxInterceptor interceptor = new TxInterceptor();
            interceptor.init(table, config, coordinator, this.cache.getAdvancedCache().getRpcManager(), recovery);
            chain.replaceInterceptor(interceptor, org.infinispan.interceptors.TxInterceptor.class);
        }

        XAResourceRecoveryRegistry recoveryRegistry = this.dependencies.getRecoveryRegistry();
        if (recoveryRegistry != null) {
            this.recovery = new InfinispanXAResourceRecovery(this.name, container);
            recoveryRegistry.addXAResourceRecovery(this.recovery);
        }
        log.debugf("%s cache started", this.name);
    }

    @Override
    public void stop(StopContext context) {
        if ((this.cache != null) && this.cache.getStatus().allowInvocations()) {
            if (this.recovery != null) {
                this.dependencies.getRecoveryRegistry().removeXAResourceRecovery(this.recovery);
            }

            this.cache.stop();
            log.debugf("%s cache stopped", this.name);
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
            return this.container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName() + "." + this.cacheName;
        }
    }
}
