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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

/**
 * {@code ServerSocketFactory} implementation creating sockets, which automatically register
 * and unregister itself at the {@code SocketBindingManager} when bound or closed.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class ManagedSocketFactory extends SocketFactory {

    /**
     * Create a socket.
     *
     * @param name the socket name
     * @return the socket
     * @throws IOException
     * @see {@linkplain SocketFactory#createSocket()}
     */
    public abstract Socket createSocket(String name) throws IOException;

    /**
     * Create a socket.
     *
     * @param name the socket-binding name.
     * @param host the host
     * @param port the port
     * @return the socket
     * @throws IOException
     * @throws UnknownHostException
     * @see {@linkplain SocketFactory#createSocket(String, int)}
     */
    public abstract Socket createSocket(String name, String host, int port) throws IOException;

    /**
     * Create a socket.
     *
     * @param name the socket-binding name.
     * @param host the host
     * @param port the port
     * @return the socket
     * @throws IOException
     * @see {@linkplain SocketFactory#createSocket(InetAddress, int)}
     */
    public abstract Socket createSocket(String name, InetAddress host, int port) throws IOException;

    /**
     * Create a socket.
     *
     * @param name the socket-binding name.
     * @param host the host
     * @param port the port
     * @param localHost the local host
     * @param localPort the local port
     * @return the socket
     * @throws IOException
     * @throws UnknownHostException
     * @see {@linkplain SocketFactory#createSocket(String, int, java.net.InetAddress, int)}
     */
    public abstract Socket createSocket(String name, String host, int port, InetAddress localHost, int localPort) throws IOException;

    /**
     * Create a socket.
     *
     * @param name the socket-binding name.
     * @param address the address
     * @param port the port
     * @param localAddress the local address
     * @param localPort the local port
     * @return the socket
     * @throws IOException
     * @see {@linkplain SocketFactory#createSocket(java.net.InetAddress, int, java.net.InetAddress, int)}
     */
    public abstract Socket createSocket(String name, InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException;

}
