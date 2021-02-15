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

package org.wildfly.clustering.marshalling.protostream.net;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.infinispan.protostream.impl.WireFormat;
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
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index >= RESOLVED_ADDRESS_INDEX && index < UNRESOLVED_HOST_INDEX) {
                result = new InetSocketAddress(InetAddressMarshaller.INSTANCE.readField(reader, index - RESOLVED_ADDRESS_INDEX, result.getAddress()), result.getPort());
            } else if (index >= UNRESOLVED_HOST_INDEX && index < PORT_INDEX) {
                result = InetSocketAddress.createUnresolved(reader.readString(), result.getPort());
            } else if (index == PORT_INDEX) {
                int port = reader.readUInt32();
                result = result.isUnresolved() ? InetSocketAddress.createUnresolved(result.getHostName(), port) : new InetSocketAddress(result.getAddress(), port);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
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
