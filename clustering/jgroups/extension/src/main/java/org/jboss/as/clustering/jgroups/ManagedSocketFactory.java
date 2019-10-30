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
import java.util.Map;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.util.SocketFactory;

/**
 * Manages registration of all JGroups sockets with a {@link SocketBindingManager}.
 * @author Paul Ferraro
 */
public class ManagedSocketFactory implements SocketFactory {

    private final SocketBindingManager manager;
    // Maps a JGroups service name its associated SocketBinding
    private final Map<String, SocketBinding> socketBindings;

    public ManagedSocketFactory(SocketBindingManager manager, Map<String, SocketBinding> socketBindings) {
        this.manager = manager;
        this.socketBindings = socketBindings;
    }

    private String getSocketBindingName(String name) {
        SocketBinding socketBinding = this.socketBindings.get(name);
        return (socketBinding != null) ? socketBinding.getName() : name;
    }

    @Override
    public Socket createSocket(String name) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getSocketFactory().createSocket(socketBindingName);
    }

    @Override
    public Socket createSocket(String name, String host, int port) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getSocketFactory().createSocket(socketBindingName, host, port);
    }

    @Override
    public Socket createSocket(String name, InetAddress address, int port) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getSocketFactory().createSocket(socketBindingName, address, port);
    }

    @Override
    public Socket createSocket(String name, String host, int port, InetAddress localHost, int localPort) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getSocketFactory().createSocket(socketBindingName, host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(String name, InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getSocketFactory().createSocket(socketBindingName, address, port, localAddress, localPort);
    }

    @Override
    public ServerSocket createServerSocket(String name) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getServerSocketFactory().createServerSocket(socketBindingName);
    }

    @Override
    public ServerSocket createServerSocket(String name, int port) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getServerSocketFactory().createServerSocket(socketBindingName, port);
    }

    @Override
    public ServerSocket createServerSocket(String name, int port, int backlog) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getServerSocketFactory().createServerSocket(socketBindingName, port, backlog);
    }

    @Override
    public ServerSocket createServerSocket(String name, int port, int backlog, InetAddress ifAddress) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.getServerSocketFactory().createServerSocket(socketBindingName, port, backlog, ifAddress);
    }

    @Override
    public DatagramSocket createDatagramSocket(String name) throws SocketException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.createDatagramSocket(socketBindingName);
    }

    @Override
    public DatagramSocket createDatagramSocket(String name, SocketAddress address) throws SocketException {
        if (address == null) return this.createDatagramSocket(name);
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.createDatagramSocket(socketBindingName, address);
    }

    @Override
    public DatagramSocket createDatagramSocket(String name, int port) throws SocketException {
        return this.createDatagramSocket(name, new InetSocketAddress(port));
    }

    @Override
    public DatagramSocket createDatagramSocket(String name, int port, InetAddress address) throws SocketException {
        return this.createDatagramSocket(name, new InetSocketAddress(address, port));
    }

    @Override
    public MulticastSocket createMulticastSocket(String name) throws IOException {
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.createMulticastSocket(socketBindingName);
    }

    @Override
    public MulticastSocket createMulticastSocket(String name, int port) throws IOException {
        return this.createMulticastSocket(name, new InetSocketAddress(port));
    }

    @Override
    public MulticastSocket createMulticastSocket(String name, SocketAddress address) throws IOException {
        if (address == null) return this.createMulticastSocket(name);
        String socketBindingName = this.getSocketBindingName(name);
        return this.manager.createMulticastSocket(socketBindingName, address);
    }

    @Override
    public void close(Socket socket) throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void close(ServerSocket socket) throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public void close(DatagramSocket socket) {
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public Map<Object, String> getSockets() {
        throw new UnsupportedOperationException();
    }
}
