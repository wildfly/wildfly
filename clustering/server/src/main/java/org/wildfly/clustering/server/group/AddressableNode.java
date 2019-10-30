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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;

import org.jgroups.Address;
import org.wildfly.clustering.group.Node;

/**
 * Node implementation that associates a JGroups {@link Address} with its logical name
 * and transport socket binding.
 * @author Paul Ferraro
 */
public class AddressableNode implements Node, Comparable<AddressableNode>, Serializable {
    private static final long serialVersionUID = -7707210981640344598L;

    private transient Address address;
    private final String name;
    private final InetSocketAddress socketAddress;

    public AddressableNode(Address address, String name, InetSocketAddress socketAddress) {
        this.address = address;
        this.name = name;
        this.socketAddress = socketAddress;
    }

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
        return (object instanceof AddressableNode) ? this.address.equals(((AddressableNode) object).address) : false;
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

    private void writeObject(java.io.ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        AddressSerializer.INSTANCE.write(output, this.address);
    }

    private void readObject(java.io.ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        this.address = AddressSerializer.INSTANCE.read(input);
    }
}
