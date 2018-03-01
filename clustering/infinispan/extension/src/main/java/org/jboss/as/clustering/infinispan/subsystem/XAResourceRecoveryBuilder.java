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

import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability.*;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.transaction.xa.XAResource;

import org.infinispan.Cache;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.SuppliedValueService;

/**
 * Builder for a {@link XAResourceRecovery} registration.
 * @author Paul Ferraro
 */
public class XAResourceRecoveryBuilder implements Builder<XAResourceRecovery>, Supplier<XAResourceRecovery>, Consumer<XAResourceRecovery> {
    private final InjectedValue<Cache<?, ?>> cache = new InjectedValue<>();
    private final InjectedValue<XAResourceRecoveryRegistry> registry = new InjectedValue<>();
    private final PathAddress cacheAddress;

    /**
     * Constructs a new {@link XAResourceRecovery} builder.
     */
    public XAResourceRecoveryBuilder(PathAddress cacheAddress) {
        this.cacheAddress = cacheAddress;
    }

    @Override
    public XAResourceRecovery get() {
        Cache<?, ?> cache = this.cache.getValue();
        XAResourceRecovery recovery = cache.getCacheConfiguration().transaction().recovery().enabled() ? new InfinispanXAResourceRecovery(cache) : null;
        if (recovery != null) {
            this.registry.getValue().addXAResourceRecovery(recovery);
        }
        return recovery;
    }

    @Override
    public void accept(XAResourceRecovery recovery) {
        if (recovery != null) {
            this.registry.getValue().removeXAResourceRecovery(recovery);
        }
    }

    @Override
    public ServiceName getServiceName() {
        return CACHE.getServiceName(this.cacheAddress).append("recovery");
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceBuilder<XAResourceRecovery> build(ServiceTarget target) {
        Service<XAResourceRecovery> service = new SuppliedValueService<>(Function.identity(), this, this);
        return target.addService(this.getServiceName(), service)
                .addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, this.registry)
                .addDependency(CACHE.getServiceName(this.cacheAddress), (Class<Cache<?, ?>>) (Class<?>) Cache.class, this.cache)
                .setInitialMode(ServiceController.Mode.PASSIVE);
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
