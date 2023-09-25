/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.distribution;

import org.infinispan.Cache;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.remoting.transport.Address;

/**
 * {@link Locality} implementation based on a {@link ConsistentHash}.
 * @author Paul Ferraro
 */
public class ConsistentHashLocality implements Locality {

    private final KeyDistribution distribution;
    private final Address localAddress;

    @SuppressWarnings("deprecation")
    public ConsistentHashLocality(Cache<?, ?> cache, ConsistentHash hash) {
        this(cache.getAdvancedCache().getComponentRegistry().getLocalComponent(KeyPartitioner.class), hash, cache.getAdvancedCache().getDistributionManager().getCacheTopology().getLocalAddress());
    }

    public ConsistentHashLocality(KeyPartitioner partitioner, ConsistentHash hash, Address localAddress) {
        this(new ConsistentHashKeyDistribution(partitioner, hash), localAddress);
    }

    ConsistentHashLocality(KeyDistribution distribution, Address localAddress) {
        this.distribution = distribution;
        this.localAddress = localAddress;
    }

    @Override
    public boolean isLocal(Object key) {
        Address primary = this.distribution.getPrimaryOwner(key);
        return this.localAddress.equals(primary);
    }
}
