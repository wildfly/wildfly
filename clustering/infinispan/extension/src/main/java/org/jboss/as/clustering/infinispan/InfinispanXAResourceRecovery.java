/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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