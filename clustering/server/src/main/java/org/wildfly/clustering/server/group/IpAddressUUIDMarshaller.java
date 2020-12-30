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

import java.io.DataOutput;
import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.IpAddressUUID;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

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
        UUIDCapturingDataOutput output = new UUIDCapturingDataOutput();
        address.writeTo(output);
        long low = output.getLow();
        if (low != DEFAULT_LOW_BITS) {
            writer.writeSFixed64(LOW_INDEX, low);
        }
        int high = output.getHigh();
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

    /**
     * IpAddressUUID does not expose its low and high values, except via its {@link IpAddressUUID#writeTo(DataOutput)} method,
     * where these are the last long/int values written.  Obtaining these values in this convoluted way is faster than reflection.
     */
    static class UUIDCapturingDataOutput implements DataOutput {
        private long low;
        private int high;

        long getLow() {
            return this.low;
        }

        int getHigh() {
            return this.high;
        }

        @Override
        public void writeInt(int v) throws IOException {
            this.high = v;
        }

        @Override
        public void writeLong(long v) throws IOException {
            this.low = v;
        }

        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte[] b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }

        @Override
        public void writeBoolean(boolean v) throws IOException {
        }

        @Override
        public void writeByte(int v) throws IOException {
        }

        @Override
        public void writeShort(int v) throws IOException {
        }

        @Override
        public void writeChar(int v) throws IOException {
        }

        @Override
        public void writeFloat(float v) throws IOException {
        }

        @Override
        public void writeDouble(double v) throws IOException {
        }

        @Override
        public void writeBytes(String s) throws IOException {
        }

        @Override
        public void writeChars(String s) throws IOException {
        }

        @Override
        public void writeUTF(String s) throws IOException {
        }
    }
}
