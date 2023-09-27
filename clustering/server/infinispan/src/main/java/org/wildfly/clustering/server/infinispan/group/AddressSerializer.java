/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.UUID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.SerializerExternalizer;

/**
 * Serializer for a JGroups {@link Address}.
 * @author Paul Ferraro
 */
public enum AddressSerializer implements Serializer<Address> {
    INSTANCE;

    @Override
    public void write(DataOutput output, Address address) throws IOException {
        org.jgroups.util.Util.writeAddress(address, output);
    }

    @Override
    public Address read(DataInput input) throws IOException {
        try {
            return org.jgroups.util.Util.readAddress(input);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class UUIDExternalizer extends SerializerExternalizer<Address> {
        @SuppressWarnings("unchecked")
        public UUIDExternalizer() {
            super((Class<Address>) (Class<?>) UUID.class, INSTANCE);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class IpAddressExternalizer extends SerializerExternalizer<Address> {
        @SuppressWarnings("unchecked")
        public IpAddressExternalizer() {
            super((Class<Address>) (Class<?>) IpAddress.class, INSTANCE);
        }
    }
}
