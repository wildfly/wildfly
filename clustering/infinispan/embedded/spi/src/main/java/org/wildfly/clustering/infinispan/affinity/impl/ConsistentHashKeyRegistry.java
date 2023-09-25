/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.affinity.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.transport.Address;

/**
 * Registry of queues of keys with affinity to the members of a consistent hash.
 * @author Paul Ferraro
 */
public class ConsistentHashKeyRegistry<K> implements KeyRegistry<K> {

    private final Map<Address, BlockingQueue<K>> keys;

    public ConsistentHashKeyRegistry(ConsistentHash hash, Predicate<Address> filter, Supplier<BlockingQueue<K>> queueFactory) {
        List<Address> members = new ArrayList<>(hash.getMembers().size());
        for (Address address : hash.getMembers()) {
            // Only create queues for members that own segments
            if (filter.test(address) && !hash.getPrimarySegmentsForOwner(address).isEmpty()) {
                members.add(address);
            }
        }
        if (members.size() == 0) {
            this.keys = Collections.emptyMap();
        } else if (members.size() == 1) {
            Address member = members.get(0);
            this.keys = Collections.singletonMap(member, queueFactory.get());
        } else {
            this.keys = new HashMap<>();
            for (Address member : members) {
                this.keys.put(member, queueFactory.get());
            }
        }
    }

    @Override
    public Set<Address> getAddresses() {
        return this.keys.keySet();
    }

    @Override
    public BlockingQueue<K> getKeys(Address address) {
        return this.keys.get(address);
    }
}
