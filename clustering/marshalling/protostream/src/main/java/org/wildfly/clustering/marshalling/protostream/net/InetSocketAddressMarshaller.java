/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for an {@link InetSocketAddress}.
 * @author Paul Ferraro
 */
public class InetSocketAddressMarshaller implements ProtoStreamMarshaller<InetSocketAddress> {

    private static final InetSocketAddress DEFAULT = new InetSocketAddress(InetAddressMarshaller.INSTANCE.createInitialValue(), 0);

    private static final int RESOLVED_ADDRESS_INDEX = 1;
    private static final int UNRESOLVED_HOST_INDEX = InetAddressMarshaller.INSTANCE.nextIndex(RESOLVED_ADDRESS_INDEX);
    private static final int PORT_INDEX = UNRESOLVED_HOST_INDEX + 1;

    @Override
    public InetSocketAddress readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<InetAddress> addressReader = reader.createFieldSetReader(InetAddressMarshaller.INSTANCE, RESOLVED_ADDRESS_INDEX);
        InetSocketAddress result = DEFAULT;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (addressReader.contains(index)) {
                result = new InetSocketAddress(addressReader.readField(result.getAddress()), result.getPort());
            } else if (index == UNRESOLVED_HOST_INDEX) {
                result = InetSocketAddress.createUnresolved(reader.readString(), result.getPort());
            } else if (index == PORT_INDEX) {
                int port = reader.readUInt32();
                result = result.isUnresolved() ? InetSocketAddress.createUnresolved(result.getHostName(), port) : new InetSocketAddress(result.getAddress(), port);
            } else {
                reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, InetSocketAddress socketAddress) throws IOException {
        if (socketAddress.isUnresolved()) {
            writer.writeString(UNRESOLVED_HOST_INDEX, socketAddress.getHostName());
        } else {
            writer.createFieldSetWriter(InetAddressMarshaller.INSTANCE, RESOLVED_ADDRESS_INDEX).writeFields(socketAddress.getAddress());
        }
        int port = socketAddress.getPort();
        if (port != DEFAULT.getPort()) {
            writer.writeUInt32(PORT_INDEX, port);
        }
    }

    @Override
    public Class<? extends InetSocketAddress> getJavaClass() {
        return InetSocketAddress.class;
    }
}
