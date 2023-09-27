/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
