/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.infinispan.protostream.descriptors.WireType;
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
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index >= ADDRESS_INDEX && index < NAME_INDEX) {
                address = AddressMarshaller.INSTANCE.readField(reader, index - ADDRESS_INDEX, address);
            } else if (index == NAME_INDEX) {
                name = reader.readString();
            } else if (index == SOCKET_ADDRESS_INDEX) {
                IpAddress ipAddress = reader.readObject(IpAddress.class);
                socketAddress = new InetSocketAddress(ipAddress.getIpAddress(), ipAddress.getPort());
            } else {
                reader.skipField(tag);
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
