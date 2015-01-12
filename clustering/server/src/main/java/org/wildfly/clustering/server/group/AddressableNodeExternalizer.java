/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.Address;
import org.wildfly.clustering.infinispan.spi.io.AbstractSimpleExternalizer;

/**
 * Infinispan externalizer for an {@link AddressableNode}.
 * @author Paul Ferraro
 */
public class AddressableNodeExternalizer extends AbstractSimpleExternalizer<AddressableNode> {
    private static final long serialVersionUID = -7336879071713713182L;

    public AddressableNodeExternalizer() {
        super(AddressableNode.class);
    }

    @Override
    public AddressableNode readObject(ObjectInput input) throws IOException {
        try {
            Address jgroupsAddress = org.jgroups.util.Util.readAddress(input);
            String name = input.readUTF();
            byte[] address = new byte[input.readInt()];
            input.readFully(address);
            int port = input.readInt();
            return new AddressableNode(jgroupsAddress, name, new InetSocketAddress(InetAddress.getByAddress(address), port));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeObject(ObjectOutput output, AddressableNode node) throws IOException {
        try {
            org.jgroups.util.Util.writeAddress(node.getAddress(), output);
            output.writeUTF(node.getName());
            InetSocketAddress socketAddress = node.getSocketAddress();
            byte[] address = socketAddress.getAddress().getAddress();
            output.writeInt(address.length);
            output.write(address);
            output.writeInt(node.getSocketAddress().getPort());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
