/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public enum InfinispanAddressMarshaller implements FieldSetMarshaller<Address, Address> {
    INSTANCE;

    private static final int JGROUPS_ADDRESS = 0;
    private static final int FIELDS = JGROUPS_ADDRESS + AddressMarshaller.INSTANCE.getFields();

    @Override
    public Address getBuilder() {
        return LocalModeAddress.INSTANCE;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Address readField(ProtoStreamReader reader, int index, Address address) throws IOException {
        if (index >= JGROUPS_ADDRESS && index < JGROUPS_ADDRESS + AddressMarshaller.INSTANCE.getFields()) {
            return new JGroupsAddress(AddressMarshaller.INSTANCE.readField(reader, index, null));
        }
        return address;
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, Address address) throws IOException {
        if (address instanceof JGroupsAddress) {
            AddressMarshaller.INSTANCE.writeFields(writer, startIndex, ((JGroupsAddress) address).getJGroupsAddress());
        }
    }
}
