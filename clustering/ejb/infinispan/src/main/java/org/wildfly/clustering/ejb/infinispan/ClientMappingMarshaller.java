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
package org.wildfly.clustering.ejb.infinispan;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

import org.infinispan.protostream.impl.WireFormat;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.common.net.Inet;

/**
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
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
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
                    reading = (tag != 0) && reader.skipField(tag);
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
