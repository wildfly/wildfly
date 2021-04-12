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

package org.wildfly.clustering.marshalling.protostream.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivilegedActionException;
import java.util.Arrays;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.common.net.Inet;
import org.wildfly.security.ParametricPrivilegedExceptionAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Marshaller for an {@link InetAddress}.
 * @author Paul Ferraro
 */
public enum InetAddressMarshaller implements FieldSetMarshaller<InetAddress, InetAddress>, ParametricPrivilegedExceptionAction<InetAddress, String> {
    INSTANCE;

    private static final InetAddress DEFAULT = InetAddress.getLoopbackAddress();

    private static final int HOST_NAME_INDEX = 0;
    private static final int ADDRESS_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public InetAddress getBuilder() {
        return DEFAULT;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public InetAddress readField(ProtoStreamReader reader, int index, InetAddress address) throws IOException {
        switch (index) {
            case HOST_NAME_INDEX:
                try {
                    return WildFlySecurityManager.doUnchecked(reader.readString(), this);
                } catch (PrivilegedActionException e) {
                    Exception exception = e.getException();
                    if (exception instanceof IOException) {
                        throw (IOException) exception;
                    }
                    throw new IllegalStateException(e);
                }
            case ADDRESS_INDEX:
                return InetAddress.getByAddress(reader.readByteArray());
            default:
                return address;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, InetAddress address) throws IOException {
        // Determine host name without triggering reverse lookup
        String hostName = Inet.getHostNameIfResolved(address);
        // Marshal as host name, if possible
        if (hostName != null) {
            if (!hostName.equals(DEFAULT.getHostName())) {
                writer.writeString(startIndex + HOST_NAME_INDEX, hostName);
            }
        } else {
            byte[] bytes = address.getAddress();
            if (!Arrays.equals(bytes, DEFAULT.getAddress())) {
                writer.writeBytes(startIndex + ADDRESS_INDEX, address.getAddress());
            }
        }
    }

    @Override
    public InetAddress run(String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }
}
