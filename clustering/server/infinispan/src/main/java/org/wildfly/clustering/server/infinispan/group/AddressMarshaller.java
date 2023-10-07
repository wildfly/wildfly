/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.UUID;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshallers for the fields of an {@link Address}.
 * @author Paul Ferraro
 */
public enum AddressMarshaller implements FieldSetMarshaller.Simple<Address> {
    INSTANCE;

    private static final int UUID_ADDRESS_INDEX = 0;
    private static final int IP_ADDRESS_INDEX = 1;
    private static final int FIELDS = 3;

    @Override
    public Address createInitialValue() {
        return null;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Address readFrom(ProtoStreamReader reader, int index, WireType type, Address address) throws IOException {
        switch (index) {
            case UUID_ADDRESS_INDEX:
                return reader.readObject(UUID.class);
            case IP_ADDRESS_INDEX:
                return reader.readObject(IpAddress.class);
            default:
                reader.skipField(type);
                return address;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Address address) throws IOException {
        if (address instanceof IpAddress) {
            writer.writeObject(IP_ADDRESS_INDEX, address);
        } else {
            writer.writeObject(UUID_ADDRESS_INDEX, address);
        }
    }
}
