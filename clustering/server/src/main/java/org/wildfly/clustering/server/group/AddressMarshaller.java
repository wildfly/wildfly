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

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.IpAddressUUID;
import org.jgroups.util.UUID;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshallers for the fields of an {@link Address}.
 * @author Paul Ferraro
 */
public enum AddressMarshaller implements FieldSetMarshaller<Address, Address> {
    INSTANCE;

    private static final int UUID_ADDRESS_INDEX = 0;
    private static final int IP_ADDRESS_INDEX = 1;
    private static final int IP_UUID_ADDRESS_INDEX = 2;
    private static final int FIELDS = 3;

    @Override
    public Address getBuilder() {
        return null;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Address readField(ProtoStreamReader reader, int index, Address address) throws IOException {
        switch (index) {
            case UUID_ADDRESS_INDEX:
                return reader.readObject(UUID.class);
            case IP_UUID_ADDRESS_INDEX:
                return reader.readObject(IpAddressUUID.class);
            case IP_ADDRESS_INDEX:
                return reader.readObject(IpAddress.class);
            default:
                return address;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, Address address) throws IOException {
        if (address instanceof IpAddress) {
            if (address instanceof IpAddressUUID) {
                writer.writeObject(startIndex + IP_UUID_ADDRESS_INDEX, address);
            } else {
                writer.writeObject(startIndex + IP_ADDRESS_INDEX, address);
            }
        } else {
            writer.writeObject(startIndex + UUID_ADDRESS_INDEX, address);
        }
    }
}
