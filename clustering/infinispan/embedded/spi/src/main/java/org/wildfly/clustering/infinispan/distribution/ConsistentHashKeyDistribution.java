/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

import java.util.List;

import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.remoting.transport.Address;

/**
 * Key distribution functions for a specific {@link ConsistentHash}.
 * @author Paul Ferraro
 */
public class ConsistentHashKeyDistribution implements KeyDistribution {

    private final KeyPartitioner partitioner;
    private final ConsistentHash hash;

    public ConsistentHashKeyDistribution(KeyPartitioner partitioner, ConsistentHash hash) {
        this.partitioner = partitioner;
        this.hash = hash;
    }

    @Override
    public Address getPrimaryOwner(Object key) {
        int segment = this.partitioner.getSegment(key);
        return this.hash.locatePrimaryOwnerForSegment(segment);
    }

    @Override
    public List<Address> getOwners(Object key) {
        int segment = this.partitioner.getSegment(key);
        return this.hash.locateOwnersForSegment(segment);
    }
}
