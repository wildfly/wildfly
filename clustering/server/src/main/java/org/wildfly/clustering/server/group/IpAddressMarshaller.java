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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.jgroups.stack.IpAddress;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for fields of a {@link IpAddress}.
 * @author Paul Ferraro
 */
public enum IpAddressMarshaller implements FieldSetMarshaller<IpAddress, IpAddressBuilder> {
    INSTANCE;

    static final InetAddress DEFAULT_ADDRESS = InetAddress.getLoopbackAddress();
    private static final int DEFAULT_PORT = 7600; // Default TCP port

    private static final int ADDRESS_INDEX = 0;
    private static final int PORT_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public IpAddressBuilder getBuilder() {
        return new DefaultIpAddressBuilder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public IpAddressBuilder readField(ProtoStreamReader reader, int index, IpAddressBuilder builder) throws IOException {
        switch (index) {
            case ADDRESS_INDEX:
                return builder.setAddress(reader.readByteArray());
            case PORT_INDEX:
                return builder.setPort(reader.readUInt32());
            default:
                return builder;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, IpAddress address) throws IOException {
        byte[] bytes = address.getIpAddress().getAddress();
        if (!Arrays.equals(bytes, DEFAULT_ADDRESS.getAddress())) {
            writer.writeBytes(startIndex + ADDRESS_INDEX, bytes);
        }
        int port = address.getPort();
        if (port != DEFAULT_PORT) {
            writer.writeUInt32(startIndex + PORT_INDEX, port);
        }
    }

    static class DefaultIpAddressBuilder implements IpAddressBuilder {
        private InetAddress address = DEFAULT_ADDRESS;
        private int port = DEFAULT_PORT;

        @Override
        public IpAddressBuilder setAddress(byte[] address) throws UnknownHostException {
            this.address = InetAddress.getByAddress(address);
            return this;
        }

        @Override
        public IpAddressBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        @Override
        public IpAddress build() {
            return new IpAddress(this.address, this.port);
        }
    }
}
