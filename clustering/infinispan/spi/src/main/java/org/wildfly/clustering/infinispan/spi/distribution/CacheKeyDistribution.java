/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.distribution;

import java.util.List;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.remoting.transport.Address;

/**
 * Key distribution appropriate for any cache mode.
 * @author Paul Ferraro
 */
public class CacheKeyDistribution implements KeyDistribution {

    private final DistributionManager distribution;
    private final KeyPartitioner partitioner;

    @SuppressWarnings("deprecation")
    public CacheKeyDistribution(Cache<?, ?> cache) {
        this.distribution = cache.getAdvancedCache().getDistributionManager();
        this.partitioner = cache.getAdvancedCache().getComponentRegistry().getLocalComponent(KeyPartitioner.class);
    }

    @Override
    public Address getPrimaryOwner(Object key) {
        return this.getCurrentKeyDistribution().getPrimaryOwner(key);
    }

    @Override
    public List<Address> getOwners(Object key) {
        return this.getCurrentKeyDistribution().getOwners(key);
    }

    private KeyDistribution getCurrentKeyDistribution() {
        return (this.distribution != null) ? new ConsistentHashKeyDistribution(this.partitioner, this.distribution.getCacheTopology().getWriteConsistentHash()) : LocalKeyDistribution.INSTANCE;
    }
}
