/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheTopology;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.NodeFactory;

/**
 * @author Paul Ferraro
 */
public class CacheMembership implements Membership {

    private final Address localAddress;
    private final List<Address> addresses;
    private final NodeFactory<Address> factory;

    public CacheMembership(Address localAddress, CacheTopology topology, NodeFactory<Address> factory) {
        this(localAddress, topology.getActualMembers(), factory);
    }

    public CacheMembership(Address localAddress, ConsistentHash hash, NodeFactory<Address> factory) {
        this(localAddress, hash.getMembers(), factory);
    }

    public CacheMembership(EmbeddedCacheManager manager, NodeFactory<Address> factory) {
        this(manager.getAddress(), manager.getMembers(), factory);
    }

    public CacheMembership(Address localAddress, List<Address> addresses, NodeFactory<Address> factory) {
        this.localAddress = localAddress;
        this.addresses = addresses;
        this.factory = factory;
    }

    @Override
    public boolean isCoordinator() {
        return this.localAddress.equals(this.getCoordinatorAddress());
    }

    @Override
    public Node getCoordinator() {
        return this.factory.createNode(this.getCoordinatorAddress());
    }

    private Address getCoordinatorAddress() {
        return this.addresses.get(0);
    }

    @Override
    public List<Node> getMembers() {
        List<Node> members = new ArrayList<>(this.addresses.size());
        for (Address address : this.addresses) {
            Node member = this.factory.createNode(address);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }
}
