/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;
import org.wildfly.clustering.service.Builder;

/**
 * Builder for a {@link XAResourceRecovery} registration.
 * @author Paul Ferraro
 */
public class XAResourceRecoveryBuilder implements Builder<XAResourceRecovery>, Service<XAResourceRecovery> {
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<XAResourceRecoveryRegistry> registry = new InjectedValue<>();
    private final String containerName;
    private final String cacheName;

    private volatile XAResourceRecovery recovery = null;

    /**
     * Constructs a new {@link XAResourceRecovery} builder.
     */
    public XAResourceRecoveryBuilder(String containerName, String cacheName) {
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheServiceName.XA_RESOURCE_RECOVERY.getServiceName(this.containerName, this.cacheName);
    }

    @Override
    public ServiceBuilder<XAResourceRecovery> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, this.registry)
                .addDependency(CacheServiceName.CACHE.getServiceName(this.containerName), Cache.class, this.cache)
                .setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public XAResourceRecovery getValue() {
        return this.recovery;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Cache<?, ?> cache = this.cache.getValue();
        if (cache.getCacheConfiguration().transaction().recovery().enabled()) {
            this.recovery = new InfinispanXAResourceRecovery(cache);
            this.registry.getValue().addXAResourceRecovery(this.recovery);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.recovery != null) {
            this.registry.getValue().removeXAResourceRecovery(this.recovery);
        }
    }

    private static class InfinispanXAResourceRecovery implements XAResourceRecovery {
        private final Cache<?, ?> cache;

        InfinispanXAResourceRecovery(Cache<?, ?> cache) {
            this.cache = cache;
        }

        @Override
        public XAResource[] getXAResources() {
            return new XAResource[] { this.cache.getAdvancedCache().getXAResource() };
        }

        @Override
        public int hashCode() {
            return this.cache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName().hashCode() ^ this.cache.getName().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if ((object == null) || !(object instanceof InfinispanXAResourceRecovery)) return false;
            InfinispanXAResourceRecovery recovery = (InfinispanXAResourceRecovery) object;
            return this.cache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName().equals(recovery.cache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName()) && this.cache.getName().equals(recovery.cache.getName());
        }

        @Override
        public String toString() {
            return this.cache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName() + "." + this.cache.getName();
        }
    }
}
