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

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.jboss.msc.service.ServiceName;

/**
 * The socket binding manager represents a registry of all
 * active (bound) sockets.
 *
 * @author Emanuel Muckenhuber
 */
public interface SocketBindingManager {

    ServiceName SOCKET_BINDING_MANAGER = ServiceName.JBOSS.append("socket-binding-manager");

    /**
     * Get the managed server socket factory.
     *
     * @return the server socket factory
     */
    ManagedServerSocketFactory getServerSocketFactory();

    /**
     * Get the socket factory.
     *
     * @return the socket factory
     */
    ManagedSocketFactory getSocketFactory();

    /**
     * Create a datagram socket.
     *
     * @param name the name for managed binding
     * @param address the socket address
     * @return the datagram socket
     * @throws SocketException
     */
    DatagramSocket createDatagramSocket(final String name, final SocketAddress address) throws SocketException;

    /**
     * Create a datagram socket.
     *
     * @param address the socket address
     * @return the datagram socket
     * @throws SocketException
     */
    DatagramSocket createDatagramSocket(final SocketAddress address) throws SocketException;

    /**
     * Create a multicast socket.
     *
     * @param name the name for the managed binding
     * @param address the socket address
     * @return the multicast socket
     * @throws IOException
     */
    MulticastSocket createMulticastSocket(final String name, final SocketAddress address) throws IOException;

    /**
     * Create a multicast socket.
     *
     * @param address the socket address
     * @return the multicast socket
     * @throws IOException
     */
    MulticastSocket createMulticastSocket(final SocketAddress address) throws IOException;

    /**
     * Return the resolved {@link InetAddress} for the default interface.
     *
     * @return the resolve address
     */
    InetAddress getDefaultInterfaceAddress();

    /**
     * Return the {@link NetworkInterfaceBinding} for the default interface.
     *
     * @return the network interface binding
     */
    NetworkInterfaceBinding getDefaultInterfaceBinding();

    /**
     * Get the server port offset.
     * TODO move to somewhere else...
     *
     * @return the port offset
     */
    int getPortOffset();

    /**
     * Get the named binding registry.
     *
     * @return the named registry
     */
    NamedManagedBindingRegistry getNamedRegistry();

    /**
     * Get the registry for unnamed open sockets.
     *
     * @return the unnamed registry
     */
    UnnamedBindingRegistry getUnnamedRegistry();

    public interface NamedManagedBindingRegistry extends ManagedBindingRegistry {

        ManagedBinding getManagedBinding(final String name);
        boolean isRegistered(final String name);

        Closeable registerSocket(String name, Socket socket);
        Closeable registerSocket(String name, ServerSocket socket);
        Closeable registerSocket(String name, DatagramSocket socket);
        Closeable registerChannel(String name, SocketChannel channel);
        Closeable registerChannel(String name, ServerSocketChannel channel);
        Closeable registerChannel(String name, DatagramChannel channel);

        void unregisterBinding(String name);

    }

    public interface UnnamedBindingRegistry extends ManagedBindingRegistry {

        Closeable registerSocket(Socket socket);
        Closeable registerSocket(ServerSocket socket);
        Closeable registerSocket(DatagramSocket socket);
        Closeable registerChannel(SocketChannel channel);
        Closeable registerChannel(ServerSocketChannel channel);
        Closeable registerChannel(DatagramChannel channel);

        void unregisterSocket(Socket socket);
        void unregisterSocket(ServerSocket socket);
        void unregisterSocket(DatagramSocket socket);
        void unregisterChannel(SocketChannel channel);
        void unregisterChannel(ServerSocketChannel channel);
        void unregisterChannel(DatagramChannel channel);

    }

}

