/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.distribution.CacheKeyDistribution;
import org.wildfly.clustering.infinispan.distribution.KeyDistribution;
import org.wildfly.clustering.server.NodeFactory;

/**
 * Function that returns the primary owner for a given cache key.
 * @author Paul Ferraro
 */
public class PrimaryOwnerLocator<K> implements Function<K, Node> {
    private final KeyDistribution distribution;
    private final NodeFactory<Address> memberFactory;

    public PrimaryOwnerLocator(Cache<? extends K, ?> cache, NodeFactory<Address> memberFactory) {
        this(new CacheKeyDistribution(cache), memberFactory);
    }

    PrimaryOwnerLocator(KeyDistribution distribution, NodeFactory<Address> memberFactory) {
        this.distribution = distribution;
        this.memberFactory = memberFactory;
    }

    @Override
    public Node apply(K key) {
        Node member = null;
        while (member == null) {
            Address address = this.distribution.getPrimaryOwner(key);
            // This has been observed to return null mid-rebalance
            if (address != null) {
                // This can return null if member has left the cluster
                member = this.memberFactory.createNode(address);
            } else {
                Thread.yield();
            }
        }
        return member;
    }
}
