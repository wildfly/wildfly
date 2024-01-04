/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.group;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.BinaryFormatter;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.SerializerExternalizer;

/**
 * Marshalling externalizer for an {@link AddressableNode}.
 * @author Paul Ferraro
 */
public enum AddressableNodeSerializer implements Serializer<AddressableNode> {
    INSTANCE;

    @Override
    public void write(DataOutput output, AddressableNode node) throws IOException {
        AddressSerializer.INSTANCE.write(output, node.getAddress());
        output.writeUTF(node.getName());
        if (!(node.getAddress() instanceof IpAddress)) {
            InetSocketAddress socketAddress = node.getSocketAddress();
            // Socket address will always contain a resolved address
            byte[] address = socketAddress.getAddress().getAddress();
            IndexSerializer.UNSIGNED_BYTE.writeInt(output, address.length);
            output.write(address);
            IndexSerializer.UNSIGNED_SHORT.writeInt(output, socketAddress.getPort());
        }
    }

    @Override
    public AddressableNode read(DataInput input) throws IOException {
        Address jgroupsAddress = AddressSerializer.INSTANCE.read(input);
        String name = input.readUTF();
        if (jgroupsAddress instanceof IpAddress) {
            return new AddressableNode((IpAddress) jgroupsAddress, name);
        }
        byte[] address = new byte[IndexSerializer.UNSIGNED_BYTE.readInt(input)];
        input.readFully(address);
        int port = IndexSerializer.UNSIGNED_SHORT.readInt(input);
        return new AddressableNode(jgroupsAddress, name, new InetSocketAddress(InetAddress.getByAddress(address), port));
    }

    @MetaInfServices(Externalizer.class)
    public static class AddressableNodeExternalizer extends SerializerExternalizer<AddressableNode> {
        public AddressableNodeExternalizer() {
            super(AddressableNode.class, INSTANCE);
        }
    }

    @MetaInfServices(Formatter.class)
    public static class AddressableNodeFormatter extends BinaryFormatter<AddressableNode> {
        public AddressableNodeFormatter() {
            super(AddressableNode.class, INSTANCE);
        }
    }
}
