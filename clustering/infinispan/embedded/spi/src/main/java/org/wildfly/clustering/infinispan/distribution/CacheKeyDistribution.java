/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

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
