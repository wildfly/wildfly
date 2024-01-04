/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.net;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class InetSocketAddressExternalizer implements Externalizer<InetSocketAddress> {

    @Override
    public void writeObject(ObjectOutput output, InetSocketAddress socketAddress) throws IOException {
        InetAddress address = socketAddress.getAddress();
        NetExternalizerProvider.INET_ADDRESS.writeObject(output, address);
        IndexSerializer.UNSIGNED_SHORT.writeInt(output, socketAddress.getPort());
        if (address == null) {
            output.writeUTF(socketAddress.getHostName());
        }
    }

    @Override
    public InetSocketAddress readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        InetAddress address = NetExternalizerProvider.INET_ADDRESS.cast(InetAddress.class).readObject(input);
        int port = IndexSerializer.UNSIGNED_SHORT.readInt(input);
        return (address != null) ? new InetSocketAddress(address, port) : InetSocketAddress.createUnresolved(input.readUTF(), port);
    }

    @Override
    public Class<InetSocketAddress> getTargetClass() {
        return InetSocketAddress.class;
    }

    @Override
    public OptionalInt size(InetSocketAddress socketAddress) {
        int size = NetExternalizerProvider.INET_ADDRESS.size(socketAddress.getAddress()).getAsInt() + IndexSerializer.UNSIGNED_SHORT.size(socketAddress.getPort());
        if (socketAddress.getAddress() == null) {
            size += socketAddress.getHostName().length() + 1;
        }
        return OptionalInt.of(size);
    }
}
