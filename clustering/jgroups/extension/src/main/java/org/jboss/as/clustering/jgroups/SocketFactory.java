/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.jgroups.util.Util;

/**
 * Provides default implementations for most {@link org.jgroups.util.SocketFactory} methods.
 * @author Paul Ferraro
 */
public interface SocketFactory extends org.jgroups.util.SocketFactory {

    static final int DEFAULT_BACKLOG = 0;
    static final int DEFAULT_BIND_PORT = 0;

    @Override
    default Socket createSocket(String name, String host, int port) throws IOException {
        return this.createSocket(name, new InetSocketAddress(host, port), null);
    }

    @Override
    default Socket createSocket(String name, InetAddress address, int port) throws IOException {
        return this.createSocket(name, new InetSocketAddress(address, port), null);
    }

    @Override
    default Socket createSocket(String name, String host, int port, InetAddress bindAddress, int bindPort) throws IOException {
        return this.createSocket(name, new InetSocketAddress(host, port), new InetSocketAddress(bindAddress, bindPort));
    }

    @Override
    default Socket createSocket(String name, InetAddress address, int port, InetAddress bindAddress, int bindPort) throws IOException {
        return this.createSocket(name, new InetSocketAddress(address, port), new InetSocketAddress(bindAddress, bindPort));
    }

    default Socket createSocket(String name, SocketAddress connectAddress, SocketAddress bindAddress) throws IOException {
        Socket socket = this.createSocket(name);
        try {
            if (bindAddress != null) {
                socket.bind(bindAddress);
            }
            socket.connect(connectAddress);
        } catch (IOException e) {
            this.close(socket);
            throw e;
        }
        return socket;
    }

    @Override
    default ServerSocket createServerSocket(String name, int port) throws IOException {
        return this.createServerSocket(name, port, DEFAULT_BACKLOG);
    }

    @Override
    default ServerSocket createServerSocket(String name, int port, int backlog) throws IOException {
        return this.createServerSocket(name, new InetSocketAddress(port), backlog);
    }

    @Override
    default ServerSocket createServerSocket(String name, int port, int backlog, InetAddress address) throws IOException {
        return this.createServerSocket(name, new InetSocketAddress(address, port), backlog);
    }

    default ServerSocket createServerSocket(String name, SocketAddress bindAddress, int backlog) throws IOException {
        ServerSocket socket = this.createServerSocket(name);
        try {
            socket.bind(bindAddress, backlog);
        } catch (IOException e) {
            this.close(socket);
            throw e;
        }
        return socket;
    }

    @Override
    default SocketChannel createSocketChannel(String name, SocketAddress bindAddress) throws IOException {
        SocketChannel channel = this.createSocketChannel(name);
        try {
            channel.bind(bindAddress);
        } catch (IOException e) {
            this.close(channel);
            throw e;
        }
        return channel;
    }

    @Override
    default ServerSocketChannel createServerSocketChannel(String name, int port) throws IOException {
        return this.createServerSocketChannel(name, port, DEFAULT_BACKLOG);
    }

    @Override
    default ServerSocketChannel createServerSocketChannel(String name, int port, int backlog) throws IOException {
        return this.createServerSocketChannel(name, new InetSocketAddress(port), backlog);
    }

    @Override
    default ServerSocketChannel createServerSocketChannel(String name, int port, int backlog, InetAddress address) throws IOException {
        return this.createServerSocketChannel(name, new InetSocketAddress(address, port), backlog);
    }

    default ServerSocketChannel createServerSocketChannel(String name, SocketAddress bindAddress, int backlog) throws IOException {
        ServerSocketChannel channel = this.createServerSocketChannel(name);
        try {
            channel.bind(bindAddress, backlog);
        } catch (IOException e) {
            this.close(channel);
            throw e;
        }
        return channel;
    }

    @Override
    default DatagramSocket createDatagramSocket(String name) throws SocketException {
        return this.createDatagramSocket(name, DEFAULT_BIND_PORT);
    }

    @Override
    default DatagramSocket createDatagramSocket(String name, int port) throws SocketException {
        return this.createDatagramSocket(name, new InetSocketAddress(port));
    }

    @Override
    default DatagramSocket createDatagramSocket(String name, int port, InetAddress address) throws SocketException {
        return this.createDatagramSocket(name, new InetSocketAddress(address, port));
    }

    @Override
    default MulticastSocket createMulticastSocket(String name) throws IOException {
        return this.createMulticastSocket(name, DEFAULT_BIND_PORT);
    }

    @Override
    default MulticastSocket createMulticastSocket(String name, int port) throws IOException {
        return this.createMulticastSocket(name, new InetSocketAddress(port));
    }

    default MulticastSocket createMulticastSocket(String name, int port, InetAddress address) throws IOException {
        return this.createMulticastSocket(name, new InetSocketAddress(address, port));
    }

    @Override
    default void close(Socket socket) throws IOException {
        Util.close(socket);
    }

    @Override
    default void close(ServerSocket socket) throws IOException {
        Util.close(socket);
    }

    @Override
    default void close(DatagramSocket socket) {
        Util.close(socket);
    }
}
