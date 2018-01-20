/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.net;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class InetSocketAddressExternalizer implements Externalizer<InetSocketAddress> {

    @Override
    public void writeObject(ObjectOutput output, InetSocketAddress socketAddress) throws IOException {
        InetAddress address = socketAddress.getAddress();
        DefaultExternalizer.INET_ADDRESS.writeObject(output, address);
        IndexSerializer.UNSIGNED_SHORT.writeInt(output, socketAddress.getPort());
        if (address == null) {
            output.writeUTF(socketAddress.getHostName());
        }
    }

    @Override
    public InetSocketAddress readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        InetAddress address = DefaultExternalizer.INET_ADDRESS.cast(InetAddress.class).readObject(input);
        int port = IndexSerializer.UNSIGNED_SHORT.readInt(input);
        return (address != null) ? new InetSocketAddress(address, port) : InetSocketAddress.createUnresolved(input.readUTF(), port);
    }

    @Override
    public Class<InetSocketAddress> getTargetClass() {
        return InetSocketAddress.class;
    }
}
