/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.group;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.topology.CacheTopology;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.spi.NodeFactory;

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

    public CacheMembership(Transport transport, NodeFactory<Address> factory) {
        this(transport.getAddress(), transport.getMembers(), factory);
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
            members.add(this.factory.createNode(address));
        }
        return members;
    }
}
