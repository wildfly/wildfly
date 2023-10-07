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
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class AddressableNodeMarshaller implements ProtoStreamMarshaller<AddressableNode> {

    private static final int ADDRESS_INDEX = 1;
    private static final int NAME_INDEX = AddressMarshaller.INSTANCE.nextIndex(ADDRESS_INDEX);
    private static final int SOCKET_ADDRESS_INDEX = NAME_INDEX + 1;

    @Override
    public AddressableNode readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<Address> addressReader = reader.createFieldSetReader(AddressMarshaller.INSTANCE, ADDRESS_INDEX);
        Address address = null;
        String name = null;
        InetSocketAddress socketAddress = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (addressReader.contains(index)) {
                address = addressReader.readField(address);
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
        writer.createFieldSetWriter(AddressMarshaller.INSTANCE, ADDRESS_INDEX).writeFields(address);
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
