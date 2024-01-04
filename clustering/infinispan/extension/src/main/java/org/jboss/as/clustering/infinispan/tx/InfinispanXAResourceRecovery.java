/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.tx;

import javax.transaction.xa.XAResource;

import org.infinispan.Cache;
import org.jboss.tm.XAResourceRecovery;

/**
 * {@link XAResourceRecovery} for an Infinispan cache.
 * @author Paul Ferraro
 */
public class InfinispanXAResourceRecovery implements XAResourceRecovery {
    private final Cache<?, ?> cache;

    public InfinispanXAResourceRecovery(Cache<?, ?> cache) {
        this.cache = cache;
    }

    @Override
    public XAResource[] getXAResources() {
        return new XAResource[] { this.cache.getAdvancedCache().getXAResource() };
    }

    @Override
    public int hashCode() {
        return this.cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName().hashCode() ^ this.cache.getName().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof InfinispanXAResourceRecovery)) return false;
        InfinispanXAResourceRecovery recovery = (InfinispanXAResourceRecovery) object;
        return this.cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName().equals(recovery.cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName()) && this.cache.getName().equals(recovery.cache.getName());
    }

    @Override
    public String toString() {
        return this.cache.getCacheManager().getCacheManagerConfiguration().cacheManagerName() + "." + this.cache.getName();
    }
}