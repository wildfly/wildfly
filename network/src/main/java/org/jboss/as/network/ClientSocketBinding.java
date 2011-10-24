/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.network;

import org.jboss.msc.service.ServiceName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author Jaikiran Pai
 */
public class ClientSocketBinding {

    public static final ServiceName CLIENT_SOCKET_BINDING_BASE_SERVICE_NAME = ServiceName.JBOSS.append("client-socket-binding");

    private final String name;
    private final SocketBindingManager socketBindingManager;
    private final boolean fixedSourcePort;
    private final NetworkInterfaceBinding sourceNetworkInterface;
    private final Integer sourcePort;
    private final InetAddress destinationAddress;
    private final int destinationPort;

    public ClientSocketBinding(final String name, final SocketBindingManager socketBindingManager,
                               final InetAddress destinationAddress, final int destinationPort,
                               final NetworkInterfaceBinding sourceNetworkInterface, final Integer sourcePort,
                               final boolean fixedSourcePort) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Socket name cannot be null or an empty string");
        }
        if (socketBindingManager == null) {
            throw new IllegalArgumentException("SocketBindingManager cannot be null for client socket binding " + name);
        }
        if (destinationAddress == null) {
            throw new IllegalArgumentException("Destination address cannot be null for client socket binding " + name);
        }
        if (destinationPort < 0) {
            throw new IllegalArgumentException("Destination port cannot be a negative value: " + destinationPort
                    + " for client socket binding " + name);
        }
        this.name = name;
        this.socketBindingManager = socketBindingManager;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.sourceNetworkInterface = sourceNetworkInterface;
        this.sourcePort = sourcePort;
        this.fixedSourcePort = fixedSourcePort;
    }

    public Socket connect() throws IOException {
        final Socket socket = this.createSocket();
        final InetAddress destinationAddress = this.getDestinationAddress();
        final int destinationPort = this.getDestinationPort();
        final SocketAddress destination = new InetSocketAddress(destinationAddress, destinationPort);
        socket.connect(destination);

        return socket;
    }

    public InetAddress getDestinationAddress() {
        return this.destinationAddress;
    }

    public int getDestinationPort() {
        return this.destinationPort;
    }

    public boolean isFixedSourcePort() {
        return this.fixedSourcePort;
    }

    public InetAddress getSourceAddresss() {
        return this.sourceNetworkInterface == null ? null : this.sourceNetworkInterface.getAddress();
    }

    public Integer getSourcePort() {
        return this.sourcePort;
    }

    Integer getAbsoluteSourcePort() {
        if (this.sourcePort == null) {
            return null;
        }
        if (this.fixedSourcePort) {
            return this.sourcePort;
        }
        final int portOffset = this.socketBindingManager.getPortOffset();
        return this.sourcePort + portOffset;
    }

    public void close() throws IOException {
        final ManagedBinding binding = this.socketBindingManager.getNamedRegistry().getManagedBinding(this.name);
        if (binding == null) {
            return;
        }
        binding.close();
    }

    public boolean isBound() {
        return this.socketBindingManager.getNamedRegistry().getManagedBinding(this.name) != null;
    }

    void setFixedSourcePort(final boolean fixedSourcePort) {

    }

    // At this point, don't really expose this createSocket() method and let's just expose
    // the connect() method, since the caller can actually misuse the returned Socket
    // to connect any random destination address/port.
    private Socket createSocket() throws IOException {
        final ManagedSocketFactory socketFactory = this.socketBindingManager.getSocketFactory();
        final Socket socket = socketFactory.createSocket(this.name);
        // if the client binding specifies the source to use, then bind this socket to the
        // appropriate source
        final SocketAddress sourceSocketAddress = this.getOptionalSourceSocketAddress();
        if (sourceSocketAddress != null) {
            socket.bind(sourceSocketAddress);
        }
        return socket;
    }

    private SocketAddress getOptionalSourceSocketAddress() {
        final InetAddress sourceAddress = this.getSourceAddresss();
        final Integer absoluteSourcePort = this.getAbsoluteSourcePort();
        if (sourceAddress == null && absoluteSourcePort == null) {
            return null;
        }
        if (sourceAddress == null) {
            return new InetSocketAddress(absoluteSourcePort);
        }
        return new InetSocketAddress(sourceAddress, absoluteSourcePort);
    }

}
