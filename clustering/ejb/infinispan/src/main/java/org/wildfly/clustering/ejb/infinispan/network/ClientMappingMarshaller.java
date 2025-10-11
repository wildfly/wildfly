/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.network;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

import io.smallrye.common.net.Inet;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link ClientMapping}.
 * @author Paul Ferraro
 */
public class ClientMappingMarshaller implements ProtoStreamMarshaller<ClientMapping> {

    private static final InetAddress DEFAULT_SOURCE_ADDRESS = (InetAddress.getLoopbackAddress() instanceof Inet4Address) ? Inet.INET4_ANY : Inet.INET6_ANY;
    private static final int DEFAULT_SOURCE_MASK = 0;
    private static final String DEFAULT_DESTINATION_ADDRESS = Inet.toURLString(InetAddress.getLoopbackAddress(), true);
    private static final int DEFAULT_DESINATION_PORT = 8080;

    private static final int SOURCE_ADDRESS_INDEX = 1;
    private static final int SOURCE_MASK_INDEX = 2;
    private static final int DESTINATION_ADDRESS_INDEX = 3;
    private static final int DESTINATION_PORT_INDEX = 4;

    @Override
    public ClientMapping readFrom(ProtoStreamReader reader) throws IOException {
        InetAddress sourceAddress = DEFAULT_SOURCE_ADDRESS;
        int sourceMask = DEFAULT_SOURCE_MASK;
        String destinationAddress = DEFAULT_DESTINATION_ADDRESS;
        int destinationPort = DEFAULT_DESINATION_PORT;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case SOURCE_ADDRESS_INDEX:
                    sourceAddress = reader.readObject(InetAddress.class);
                    break;
                case SOURCE_MASK_INDEX:
                    sourceMask = reader.readUInt32();
                    break;
                case DESTINATION_ADDRESS_INDEX:
                    destinationAddress = reader.readString();
                    break;
                case DESTINATION_PORT_INDEX:
                    destinationPort = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new ClientMapping(sourceAddress, sourceMask, destinationAddress, destinationPort);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ClientMapping mapping) throws IOException {
        InetAddress address = mapping.getSourceNetworkAddress();
        if (!address.equals(DEFAULT_SOURCE_ADDRESS)) {
            writer.writeObject(SOURCE_ADDRESS_INDEX, address);
        }
        int sourceNetworkMask = mapping.getSourceNetworkMaskBits();
        if (sourceNetworkMask != DEFAULT_SOURCE_MASK) {
            writer.writeUInt32(SOURCE_MASK_INDEX, sourceNetworkMask);
        }
        String destinationAddress = mapping.getDestinationAddress();
        if (!destinationAddress.equals(DEFAULT_DESTINATION_ADDRESS)) {
            writer.writeString(DESTINATION_ADDRESS_INDEX, mapping.getDestinationAddress());
        }
        int destinationPort = mapping.getDestinationPort();
        if (destinationPort != DEFAULT_DESINATION_PORT) {
            writer.writeUInt32(DESTINATION_PORT_INDEX, destinationPort);
        }
    }

    @Override
    public Class<? extends ClientMapping> getJavaClass() {
        return ClientMapping.class;
    }
}
