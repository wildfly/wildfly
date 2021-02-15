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
