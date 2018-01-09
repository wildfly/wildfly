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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.Address;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.persistence.BinaryKeyFormat;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
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
        InetSocketAddress socketAddress = node.getSocketAddress();
        // Socket address will always contain a resolved address
        byte[] address = socketAddress.getAddress().getAddress();
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, address.length);
        output.write(address);
        IndexSerializer.UNSIGNED_SHORT.writeInt(output, socketAddress.getPort());
    }

    @Override
    public AddressableNode read(DataInput input) throws IOException {
        Address jgroupsAddress = AddressSerializer.INSTANCE.read(input);
        String name = input.readUTF();
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

    @MetaInfServices(KeyFormat.class)
    public static class AddressableNodeKeyFormat extends BinaryKeyFormat<AddressableNode> {
        public AddressableNodeKeyFormat() {
            super(AddressableNode.class, INSTANCE);
        }
    }
}
