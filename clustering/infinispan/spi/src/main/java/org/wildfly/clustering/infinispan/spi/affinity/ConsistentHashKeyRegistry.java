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

package org.wildfly.clustering.infinispan.spi.affinity;

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
