/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.stack.IpAddress;
import org.wildfly.clustering.group.Node;

/**
 * Node factory implementation.  Node instances are cached and invalidated by the {@link org.wildfly.clustering.group.Group} when appropriate.
 * @author Paul Ferraro
 */
public class ChannelNodeFactoryImpl implements ChannelNodeFactory, AutoCloseable {

    private final ConcurrentMap<Address, Node> nodes = new ConcurrentHashMap<>();
    private final Channel channel;

    public ChannelNodeFactoryImpl(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Node createNode(Address address) {
        Node node = this.nodes.get(address);
        if (node != null) return node;

        IpAddress ipAddress = (IpAddress) this.channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, address));
        InetSocketAddress socketAddress = new InetSocketAddress(ipAddress.getIpAddress(), ipAddress.getPort());
        String name = this.channel.getName(address);
        if (name == null) {
            name = String.format("%s:%s", socketAddress.getHostString(), socketAddress.getPort());
        }
        node = new AddressableNode(address, name, socketAddress);
        Node existing = this.nodes.putIfAbsent(address, node);
        return (existing != null) ? existing : node;
    }

    @Override
    public void close() {
        this.nodes.clear();
    }

    @Override
    public void invalidate(Collection<Address> addresses) {
        if (!addresses.isEmpty()) {
            this.nodes.keySet().removeAll(addresses);
        }
    }
}
