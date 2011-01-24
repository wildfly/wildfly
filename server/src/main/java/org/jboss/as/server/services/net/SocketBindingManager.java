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
package org.jboss.as.server.services.net;

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
import java.util.Collection;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public interface SocketBindingManager {

    ServiceName SOCKET_BINDING_MANAGER = ServiceName.JBOSS.append("socket-binding-manager");

    /**
     * Get the managed server socket factory.
     *
     * @return the server socket factory
     */
    ServerSocketFactory getServerSocketFactory();

    /**
     * Get the socket factory.
     *
     * @return the socket factory
     */
    SocketFactory getSocketFactory();

    /**
     * Create a datagram socket.
     *
     * @param address the socket address
     * @return the datagram socket
     * @throws SocketException
     */
    DatagramSocket createDatagramSocket(final SocketAddress address) throws SocketException ;

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
     * Get the server port offset.
     * TODO move to somewhere else...
     *
     * @return the port offset
     */
    int getPortOffset();

    /**
     * List the activate bindings.
     *
     * @return the registered bindings
     */
    Collection<ManagedBinding> listActiveBindings();

    /**
     * Register an active socket binding.
     *
     * @param binding the managed binding
     * @param bindingName the binding name
     */
    Closeable registerBinding(final ManagedBinding binding);

    Closeable registerSocket(final Socket socket);
    Closeable registerSocket(final ServerSocket socket);
    Closeable registerSocket(final DatagramSocket socket);
    Closeable registerChannel(final SocketChannel channel);
    Closeable registerChannel(final ServerSocketChannel channel);
    Closeable registerChannel(final DatagramChannel channel);

    /**
     * Unregister a socket binding.
     *
     * @param binding the managed socket binding
     */
    void unregisterBinding(ManagedBinding binding);

    void unregisterSocket(final Socket socket);
    void unregisterSocket(final ServerSocket socket);
    void unregisterSocket(final DatagramSocket socket);
    void unregisterChannel(final SocketChannel channel);
    void unregisterChannel(final ServerSocketChannel channel);
    void unregisterChannel(final DatagramChannel channel);

}

