/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.IpAddressUUID;
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

    @MetaInfServices(Externalizer.class)
    public static class IpAddressUUIDExternalizer extends SerializerExternalizer<Address> {
        @SuppressWarnings("unchecked")
        public IpAddressUUIDExternalizer() {
            super((Class<Address>) (Class<?>) IpAddressUUID.class, INSTANCE);
        }
    }
}
