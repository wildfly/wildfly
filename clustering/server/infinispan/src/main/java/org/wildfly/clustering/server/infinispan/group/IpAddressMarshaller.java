/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.infinispan.protostream.descriptors.WireType;
import org.jgroups.stack.IpAddress;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for fields of a {@link IpAddress}.
 * @author Paul Ferraro
 */
public enum IpAddressMarshaller implements FieldSetMarshaller.Supplied<IpAddress, IpAddressBuilder> {
    INSTANCE;

    static final InetAddress DEFAULT_ADDRESS = InetAddress.getLoopbackAddress();
    private static final int DEFAULT_PORT = 7600; // Default TCP port

    private static final int ADDRESS_INDEX = 0;
    private static final int PORT_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public IpAddressBuilder createInitialValue() {
        return new DefaultIpAddressBuilder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public IpAddressBuilder readFrom(ProtoStreamReader reader, int index, WireType type, IpAddressBuilder builder) throws IOException {
        switch (index) {
            case ADDRESS_INDEX:
                return builder.setAddress(reader.readByteArray());
            case PORT_INDEX:
                return builder.setPort(reader.readUInt32());
            default:
                reader.skipField(type);
                return builder;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, IpAddress address) throws IOException {
        byte[] bytes = address.getIpAddress().getAddress();
        if (!Arrays.equals(bytes, DEFAULT_ADDRESS.getAddress())) {
            writer.writeBytes(ADDRESS_INDEX, bytes);
        }
        int port = address.getPort();
        if (port != DEFAULT_PORT) {
            writer.writeUInt32(PORT_INDEX, port);
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
        public IpAddress get() {
            return new IpAddress(this.address, this.port);
        }
    }
}
