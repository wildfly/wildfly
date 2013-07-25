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
package org.wildfly.clustering.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jboss.as.clustering.infinispan.io.AbstractSimpleExternalizer;

public class SimpleNodeExternalizer extends AbstractSimpleExternalizer<SimpleNode> {
    private static final long serialVersionUID = 7862205227654119771L;

    public SimpleNodeExternalizer() {
        super(SimpleNode.class);
    }

    @Override
    public void writeObject(ObjectOutput output, SimpleNode node) throws IOException {
        output.writeUTF(node.getName());
        InetSocketAddress socketAddress = node.getSocketAddress();
        byte[] address = socketAddress.getAddress().getAddress();
        output.writeInt(address.length);
        output.write(address);
        output.writeInt(node.getSocketAddress().getPort());
    }

    @Override
    public SimpleNode readObject(ObjectInput input) throws IOException {
        String name = input.readUTF();
        byte[] address = new byte[input.readInt()];
        input.readFully(address);
        int port = input.readInt();
        return new SimpleNode(name, new InetSocketAddress(InetAddress.getByAddress(address), port));
    }
}
