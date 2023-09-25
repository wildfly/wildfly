/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.wildfly.clustering.group.Node;

/**
 * Node implementation that associates a JGroups {@link Address} with its logical name
 * and transport socket binding.
 * @author Paul Ferraro
 */
public class AddressableNode implements Node, Addressable, Comparable<AddressableNode>, Serializable {
    private static final long serialVersionUID = -7707210981640344598L;

    private transient Address address;
    private final String name;
    private final InetSocketAddress socketAddress;

    public AddressableNode(IpAddress address, String name) {
        this(address, name, new InetSocketAddress(address.getIpAddress(), address.getPort()));
    }

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
        return this.address.compareTo(node.getAddress());
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
