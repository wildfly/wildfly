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
import java.net.UnknownHostException;

/**
 * A outbound socket binding represents the client end of a socket. It represents binding from a local "host"
 * to a remote "host". In some special cases the remote host can itself be the same local host.
 * Unlike the {@link SocketBinding} which represents a {@link java.net.ServerSocket} that opens a socket for "listening",
 * the {@link OutboundSocketBinding} represents a {@link Socket} which "connects" to a remote/local host
 *
 * @author Jaikiran Pai
 */
public class OutboundSocketBinding {

    public static final ServiceName OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME = ServiceName.JBOSS.append("outbound-socket-binding");

    private final String name;
    private final SocketBindingManager socketBindingManager;
    private final boolean fixedSourcePort;
    private final NetworkInterfaceBinding sourceNetworkInterface;
    private final Integer sourcePort;
    private final String unresolvedDestinationAddress;
    private final int destinationPort;

    /**
     * The destination address is lazily resolved whenever a request is made {@link #getDestinationAddress()}
     * or for {@link #connect()}
     */
    private InetAddress resolvedDestinationAddress;

    /**
     * Creates a outbound socket binding
     *
     * @param name                   Name of the outbound socket binding
     * @param socketBindingManager   The socket binding manager
     * @param destinationAddress     The destination address to which this socket will be "connected". Cannot be null or empty string.
     * @param destinationPort        The destination port. Cannot be < 0.
     * @param sourceNetworkInterface (Optional) source network interface which will be used as the "source" of the socket binding
     * @param sourcePort             (Optional) source port. Cannot be null or < 0
     * @param fixedSourcePort        True if the <code>sourcePort</code> has to be used as a fixed port number. False if the <code>sourcePort</code>
     *                               will be added to the port offset while determining the absolute source port.
     */
    public OutboundSocketBinding(final String name, final SocketBindingManager socketBindingManager,
                                 final String destinationAddress, final int destinationPort,
                                 final NetworkInterfaceBinding sourceNetworkInterface, final Integer sourcePort,
                                 final boolean fixedSourcePort) {
        if (name == null || name.trim().isEmpty()) {
            throw NetworkMessages.MESSAGES.nullOrEmptyVar("Socket name");
        }
        if (socketBindingManager == null) {
            throw NetworkMessages.MESSAGES.nullOutboundSocketBindingParam(SocketBindingManager.class.getSimpleName(), name);
        }
        if (destinationAddress == null || destinationAddress.trim().isEmpty()) {
            throw NetworkMessages.MESSAGES.nullDestinationAddress(name);
        }
        if (destinationPort < 0) {
            throw NetworkMessages.MESSAGES.negativeDestinationPort(destinationPort, name);
        }
        this.name = name;
        this.socketBindingManager = socketBindingManager;
        this.unresolvedDestinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.sourceNetworkInterface = sourceNetworkInterface;
        this.sourcePort = sourcePort;
        this.fixedSourcePort = fixedSourcePort;
    }

    /**
     * Creates a outbound socket binding
     *
     * @param name                   Name of the outbound socket binding
     * @param socketBindingManager   The socket binding manager
     * @param destinationAddress     The destination address to which this socket will be "connected". Cannot be null.
     * @param destinationPort        The destination port. Cannot be < 0.
     * @param sourceNetworkInterface (Optional) source network interface which will be used as the "source" of the socket binding
     * @param sourcePort             (Optional) source port. Cannot be null or < 0
     * @param fixedSourcePort        True if the <code>sourcePort</code> has to be used as a fixed port number. False if the <code>sourcePort</code>
     *                               will be added to the port offset while determining the absolute source port.
     */
    public OutboundSocketBinding(final String name, final SocketBindingManager socketBindingManager,
                                 final InetAddress destinationAddress, final int destinationPort,
                                 final NetworkInterfaceBinding sourceNetworkInterface, final Integer sourcePort,
                                 final boolean fixedSourcePort) {
        this(name, socketBindingManager, destinationAddress.getHostAddress(), destinationPort, sourceNetworkInterface, sourcePort, fixedSourcePort);
        this.resolvedDestinationAddress = destinationAddress;
    }

    /**
     * Creates a {@link Socket} represented by this {@link OutboundSocketBinding} and connects to the
     * destination
     *
     * @return
     * @throws IOException
     */
    public Socket connect() throws IOException {
        final Socket socket = this.createSocket();
        final InetAddress destinationAddress = this.getDestinationAddress();
        final int destinationPort = this.getDestinationPort();
        final SocketAddress destination = new InetSocketAddress(destinationAddress, destinationPort);
        socket.connect(destination);

        return socket;
    }

    /**
     * Returns the destination address of this outbound socket binding. If the destination address
     * is already resolved then this method return that address or else it tries to resolve the
     * address before return.
     *
     * @return
     * @throws UnknownHostException If the destination address cannot be resolved
     */
    public synchronized InetAddress getDestinationAddress() throws UnknownHostException {
        if (this.resolvedDestinationAddress != null) {
            return this.resolvedDestinationAddress;
        }
        this.resolvedDestinationAddress = InetAddress.getByName(this.unresolvedDestinationAddress);
        return this.resolvedDestinationAddress;
    }

    public int getDestinationPort() {
        return this.destinationPort;
    }

    public boolean isFixedSourcePort() {
        return this.fixedSourcePort;
    }

    /**
     * Returns the source address of this outbound socket binding. If no explicit source address is specified
     * for this binding, then this method returns the address of the default interface that's configured
     * for the socket binding group
     *
     * @return
     */
    public InetAddress getSourceAddress() {
        return this.sourceNetworkInterface != null ? this.sourceNetworkInterface.getAddress() : this.socketBindingManager.getDefaultInterfaceAddress();
    }

    /**
     * The source port for this outbound socket binding. Note that this isn't the "absolute" port if the
     * this outbound socket binding has a port offset. To get the absolute source port, use the {@link #getAbsoluteSourcePort()}
     * method
     *
     * @return
     */
    public Integer getSourcePort() {
        return this.sourcePort;
    }

    /**
     * The absolute source port for this outbound socket binding. The absolute source port is the same as {@link #getSourcePort()}
     * if the outbound socket binding is marked for "fixed source port". Else, it is the sum of {@link #getSourcePort()}
     * and the port offset configured on the {@link SocketBindingManager}
     *
     * @return
     */
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

    /**
     * Closes the outbound socket binding connection
     *
     * @throws IOException
     */
    public void close() throws IOException {
        final ManagedBinding binding = this.socketBindingManager.getNamedRegistry().getManagedBinding(this.name);
        if (binding == null) {
            return;
        }
        binding.close();
    }

    /**
     * Returns true if a socket connection has been established by this outbound socket binding. Else returns false
     *
     * @return
     */
    public boolean isConnected() {
        return this.socketBindingManager.getNamedRegistry().getManagedBinding(this.name) != null;
    }

    // At this point, don't really expose this createSocket() method and let's just expose
    // the connect() method, since the caller can actually misuse the returned Socket
    // to connect any random destination address/port.
    private Socket createSocket() throws IOException {
        final ManagedSocketFactory socketFactory = this.socketBindingManager.getSocketFactory();
        final Socket socket = socketFactory.createSocket(this.name);
        // if the outbound binding specifies the source to use, then bind this socket to the
        // appropriate source
        final SocketAddress sourceSocketAddress = this.getOptionalSourceSocketAddress();
        if (sourceSocketAddress != null) {
            socket.bind(sourceSocketAddress);
        }
        return socket;
    }

    private SocketAddress getOptionalSourceSocketAddress() {
        final InetAddress sourceAddress = this.getSourceAddress();
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
