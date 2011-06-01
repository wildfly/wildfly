/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Managed {@code ServerSocket} binding, automatically registering itself
 * at the {@code SocketBindingManager} when bound.
 *
 * @author Emanuel Muckenhuber
 */
public class ManagedServerSocketBinding extends ServerSocket implements ManagedBinding {

    private final String name;
    private final SocketBindingManager socketBindings;
    private final boolean metrics;
    private volatile long acceptCount = 0;

    ManagedServerSocketBinding(final SocketBindingManager socketBindings) throws IOException {
        this(null, socketBindings, false);
    }

    ManagedServerSocketBinding(final SocketBindingManager socketBindings, final boolean metrics) throws IOException {
        this(null, socketBindings, metrics);
    }

    ManagedServerSocketBinding(final String name, final SocketBindingManager socketBindings) throws IOException {
        this(name, socketBindings, false);
    }

    ManagedServerSocketBinding(final String name, final SocketBindingManager socketBindings, final boolean metrics) throws IOException {
        this.name = name;
        this.socketBindings = socketBindings;
        this.metrics = metrics;
    }

    @Override
    public String getSocketBindingName() {
        return name;
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return InetSocketAddress.class.cast(getLocalSocketAddress());
    }

    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        super.bind(endpoint, backlog);
        if(name != null) {
            socketBindings.getNamedRegistry().registerBinding(this);
        } else {
            socketBindings.getUnnamedRegistry().registerBinding(this);
        }
    }

    @Override
    public Socket accept() throws IOException {
        final Socket socket = metrics ? new ManagedSocketBinding(socketBindings.getUnnamedRegistry()) : new Socket();
        implAccept(socket);
        if(metrics) {
            acceptCount++;
        }
        return socket;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if(name != null) {
                socketBindings.getNamedRegistry().unregisterBinding(this);
            } else {
                socketBindings.getUnnamedRegistry().unregisterBinding(this);
            }
        }
    }

    public long getAcceptCount() {
        return acceptCount;
    }

}

