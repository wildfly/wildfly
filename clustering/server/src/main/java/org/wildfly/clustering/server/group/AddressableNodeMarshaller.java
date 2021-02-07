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

package org.wildfly.clustering.server.group;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.infinispan.protostream.impl.WireFormat;
import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class AddressableNodeMarshaller implements ProtoStreamMarshaller<AddressableNode> {

    private static final int ADDRESS_INDEX = 1;
    private static final int NAME_INDEX = ADDRESS_INDEX + AddressMarshaller.INSTANCE.getFields();
    private static final int SOCKET_ADDRESS_INDEX = NAME_INDEX + 1;

    @Override
    public AddressableNode readFrom(ProtoStreamReader reader) throws IOException {
        Address address = null;
        String name = null;
        InetSocketAddress socketAddress = null;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index >= ADDRESS_INDEX && index < NAME_INDEX) {
                address = AddressMarshaller.INSTANCE.readField(reader, index - ADDRESS_INDEX, address);
            } else if (index == NAME_INDEX) {
                name = reader.readString();
            } else if (index == SOCKET_ADDRESS_INDEX) {
                IpAddress ipAddress = reader.readObject(IpAddress.class);
                socketAddress = new InetSocketAddress(ipAddress.getIpAddress(), ipAddress.getPort());
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return (address instanceof IpAddress) ? new AddressableNode((IpAddress) address, name) : new AddressableNode(address, name, socketAddress);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, AddressableNode member) throws IOException {
        Address address = member.getAddress();
        AddressMarshaller.INSTANCE.writeFields(writer, ADDRESS_INDEX, address);
        writer.writeString(NAME_INDEX, member.getName());
        if (!(address instanceof IpAddress)) {
            writer.writeObject(SOCKET_ADDRESS_INDEX, new IpAddress(member.getSocketAddress()));
        }
    }

    @Override
    public Class<? extends AddressableNode> getJavaClass() {
        return AddressableNode.class;
    }
}
