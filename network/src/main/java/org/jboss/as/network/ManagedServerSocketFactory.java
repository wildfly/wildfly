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
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

/**
 * {@code ServerSocketFactory} implementation creating sockets, which automatically register
 * and unregister itself at the {@code SocketBindingManager} when bound or closed.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class ManagedServerSocketFactory extends ServerSocketFactory {

    /**
     * Create a named server socket.
     *
     * @param name the socket-binding name
     * @return the server socket
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(String name) throws IOException;

    /**
     * Create named server socket.
     *
     * @param name the socket-binding name
     * @param port the port
     * @return the server socket
     * @throws IOException
     * @see {@linkplain ServerSocketFactory#createServerSocket(int)}
     */
    public abstract ServerSocket createServerSocket(String name, int port) throws IOException;

    /**
     * Create a named server socket.
     *
     * @param name the socket-binding name
     * @param port the port
     * @param backlog the backlog
     * @return the server socket
     * @throws IOException
     * @see {@linkplain ServerSocketFactory#createServerSocket(int, int)}
     */
    public abstract ServerSocket createServerSocket(String name, int port, int backlog) throws IOException;

    /**
     * Create a named server socket.
     *
     * @param name the socket-binding name
     * @param port the port
     * @param backlog the backlog
     * @param ifAddress the interface address
     * @return the server socket
     * @throws IOException
     * @see {@linkplain ServerSocketFactory#createServerSocket(int, int)}
     */
    public abstract ServerSocket createServerSocket(String name, int port, int backlog, InetAddress ifAddress) throws IOException;

}
