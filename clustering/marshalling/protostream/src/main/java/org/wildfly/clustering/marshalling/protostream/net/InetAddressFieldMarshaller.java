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
import java.net.InetAddress;
import java.util.Arrays;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.FieldMarshaller;

/**
 * @author Paul Ferraro
 */
public enum InetAddressFieldMarshaller implements FieldMarshaller<InetAddress, InetAddress> {
    INSTANCE;

    private static final InetAddress DEFAULT = InetAddress.getLoopbackAddress();
    private static final int HOST_NAME_INDEX = 0;
    private static final int ADDRESS_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public InetAddress readField(ImmutableSerializationContext context, RawProtoStreamReader reader, int index, InetAddress address) throws IOException {
        switch (index) {
            case HOST_NAME_INDEX:
                return InetAddress.getByName(reader.readString());
            case ADDRESS_INDEX:
                return InetAddress.getByAddress(reader.readByteArray());
            default:
                return address;
        }
    }

    @Override
    public void writeFields(ImmutableSerializationContext context, RawProtoStreamWriter writer, int startIndex, InetAddress address) throws IOException {
        // Determine host name without triggering reverse lookup
        String hostName = (address.toString().indexOf('/') > 0) ? address.getHostName() : null;
        // Marshal as host name, if possible
        if (hostName != null) {
            if (!hostName.equals(DEFAULT.getHostName())) {
                writer.writeString(startIndex + HOST_NAME_INDEX, hostName);
            }
        } else {
            byte[] bytes = address.getAddress();
            if (!Arrays.equals(bytes, DEFAULT.getAddress())) {
                writer.writeBytes(startIndex + ADDRESS_INDEX, address.getAddress());
            }
        }
    }
}
