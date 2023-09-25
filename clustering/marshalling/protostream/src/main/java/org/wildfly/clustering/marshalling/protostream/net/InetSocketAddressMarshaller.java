/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.net;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for an {@link InetSocketAddress}.
 * @author Paul Ferraro
 */
public class InetSocketAddressMarshaller implements ProtoStreamMarshaller<InetSocketAddress> {

    private static final InetSocketAddress DEFAULT = new InetSocketAddress(InetAddressMarshaller.INSTANCE.getBuilder(), 0);

    private static final int RESOLVED_ADDRESS_INDEX = 1;
    private static final int UNRESOLVED_HOST_INDEX = RESOLVED_ADDRESS_INDEX + InetAddressMarshaller.INSTANCE.getFields();
    private static final int PORT_INDEX = UNRESOLVED_HOST_INDEX + 1;

    @Override
    public InetSocketAddress readFrom(ProtoStreamReader reader) throws IOException {
        InetSocketAddress result = DEFAULT;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index >= RESOLVED_ADDRESS_INDEX && index < UNRESOLVED_HOST_INDEX) {
                result = new InetSocketAddress(InetAddressMarshaller.INSTANCE.readField(reader, index - RESOLVED_ADDRESS_INDEX, result.getAddress()), result.getPort());
            } else if (index >= UNRESOLVED_HOST_INDEX && index < PORT_INDEX) {
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
            InetAddressMarshaller.INSTANCE.writeFields(writer, RESOLVED_ADDRESS_INDEX, socketAddress.getAddress());
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
