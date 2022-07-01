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

package org.wildfly.clustering.server.infinispan.group;

import java.io.DataOutput;
import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.IpAddressUUID;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleDataOutput;

/**
 * Marshaller for {@link IpAddressUUID} addresses.
 * @author Paul Ferraro
 */
public class IpAddressUUIDMarshaller implements ProtoStreamMarshaller<IpAddressUUID> {

    private static final long DEFAULT_LOW_BITS = 0;
    private static final int DEFAULT_HIGH_BITS = 0;

    private static final int IP_ADDRESS_INDEX = 1;
    private static final int LOW_INDEX = IP_ADDRESS_INDEX + IpAddressMarshaller.INSTANCE.getFields();
    private static final int HIGH_INDEX = LOW_INDEX + 1;

    @Override
    public IpAddressUUID readFrom(ProtoStreamReader reader) throws IOException {
        IpAddressBuilder ipAddressBuilder = IpAddressMarshaller.INSTANCE.getBuilder();
        long low = DEFAULT_LOW_BITS;
        int high = DEFAULT_HIGH_BITS;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index >= IP_ADDRESS_INDEX && index < LOW_INDEX) {
                ipAddressBuilder = IpAddressMarshaller.INSTANCE.readField(reader, index - IP_ADDRESS_INDEX, ipAddressBuilder);
            } else if (index == LOW_INDEX) {
                low = reader.readSFixed64();
            } else if (index == HIGH_INDEX) {
                high = reader.readSFixed32();
            } else {
                reader.skipField(tag);
            }
        }
        return new DefaultIpAddressUUID(ipAddressBuilder.build(), low, high);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, IpAddressUUID address) throws IOException {
        IpAddressMarshaller.INSTANCE.writeFields(writer, IP_ADDRESS_INDEX, address);
        long[] longs = new long[1];
        int[] ints = new int[1];
        DataOutput output = new SimpleDataOutput.Builder().with(longs).with(ints).build();
        address.writeTo(output);
        long low = longs[0];
        if (low != DEFAULT_LOW_BITS) {
            writer.writeSFixed64(LOW_INDEX, low);
        }
        int high = ints[0];
        if (high != DEFAULT_HIGH_BITS) {
            writer.writeSFixed32(HIGH_INDEX, high);
        }
    }

    @Override
    public Class<? extends IpAddressUUID> getJavaClass() {
        return IpAddressUUID.class;
    }

    private static class DefaultIpAddressUUID extends IpAddressUUID {

        DefaultIpAddressUUID(IpAddress ipAddress, long low, int high) {
            super(ipAddress.getIpAddress(), ipAddress.getPort(), low, high);
        }
    }
}
