/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.jgroups.Addressable;
import org.jgroups.Address;
import org.wildfly.clustering.group.Node;

/**
 * Node implementation that associates a JGroups {@link Address} with its logical name
 * and transport socket binding.
 * @author Paul Ferraro
 */
public class AddressableNode implements Node, Addressable, Comparable<AddressableNode> {

    private final Address address;
    private final String name;
    private final InetSocketAddress socketAddress;

    public AddressableNode(Address address, String name, InetSocketAddress socketAddress) {
        this.address = address;
        this.name = name;
        this.socketAddress = socketAddress;
    }

    @Override
    public Address getAddress() {
        return this.address;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public int compareTo(AddressableNode node) {
        return this.address.compareTo(node.address);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Addressable) {
            Addressable node = (Addressable) object;
            return this.address.equals(node.getAddress());
        }
        return false;
    }

    @Override
    public String toString() {
        return this.address.toString();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return this.socketAddress;
    }
}
