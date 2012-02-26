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
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;

import org.jboss.as.network.SocketBindingManager;
import org.jgroups.util.SocketFactory;

/**
 * Manages registration of all JGroups sockets with a {@link SocketBindingManager}.
 * @author Paul Ferraro
 */
public class ManagedSocketFactory implements SocketFactory {

    private final SocketFactory factory;
    private final SocketBindingManager manager;

    public ManagedSocketFactory(SocketFactory factory, SocketBindingManager manager) {
        this.factory = factory;
        this.manager = manager;
    }

    @Override
    public Socket createSocket(String name) throws IOException {
        return this.register(this.factory.createSocket(name));
    }

    @Override
    public Socket createSocket(String name, String host, int port) throws IOException {
        return this.register(this.factory.createSocket(name, host, port));
    }

    @Override
    public Socket createSocket(String name, InetAddress address, int port) throws IOException {
        return this.register(this.factory.createSocket(name, address, port));
    }

    @Override
    public Socket createSocket(String name, String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return this.register(this.factory.createSocket(name, host, port, localAddress, localPort));
    }

    @Override
    public Socket createSocket(String name, InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return this.register(this.factory.createSocket(name, address, port, localAddress, localPort));
    }

    private Socket register(final Socket socket) {
        final SocketBindingManager.UnnamedBindingRegistry registry = this.manager.getUnnamedRegistry();
        registry.registerSocket(socket);
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(String name) throws IOException {
        return this.register(this.factory.createServerSocket(name));
    }

    @Override
    public ServerSocket createServerSocket(String name, int port) throws IOException {
        return this.register(this.factory.createServerSocket(name, port));
    }

    @Override
    public ServerSocket createServerSocket(String name, int port, int backlog) throws IOException {
        return this.register(this.factory.createServerSocket(name, port, backlog));
    }

    @Override
    public ServerSocket createServerSocket(String name, int port, int backlog, InetAddress bindAddr) throws IOException {
        return this.register(this.factory.createServerSocket(name, port, backlog, bindAddr));
    }

    private ServerSocket register(final ServerSocket socket) {
        final SocketBindingManager.UnnamedBindingRegistry registry = this.manager.getUnnamedRegistry();
        registry.registerSocket(socket);
        return socket;
    }

    @Override
    public DatagramSocket createDatagramSocket(String name) throws SocketException {
        return this.register(this.factory.createDatagramSocket(name));
    }

    @Override
    public DatagramSocket createDatagramSocket(String name, SocketAddress bindAddress) throws SocketException {
        return this.register(this.factory.createDatagramSocket(name, bindAddress));
    }

    @Override
    public DatagramSocket createDatagramSocket(String name, int port) throws SocketException {
        return this.register(this.factory.createDatagramSocket(name, port));
    }

    @Override
    public DatagramSocket createDatagramSocket(String name, int port, InetAddress localAddress) throws SocketException {
        return this.register(this.factory.createDatagramSocket(name, port, localAddress));
    }

    private DatagramSocket register(final DatagramSocket socket) {
        SocketBindingManager.UnnamedBindingRegistry registry = this.manager.getUnnamedRegistry();
        registry.registerSocket(socket);
        return socket;
    }

    @Override
    public MulticastSocket createMulticastSocket(String name) throws IOException {
        return this.register(this.factory.createMulticastSocket(name));
    }

    @Override
    public MulticastSocket createMulticastSocket(String name, int port) throws IOException {
        return this.register(this.factory.createMulticastSocket(name, port));
    }

    @Override
    public MulticastSocket createMulticastSocket(String name, SocketAddress bindAddress) throws IOException {
        return this.register(this.factory.createMulticastSocket(name, bindAddress));
    }

    private MulticastSocket register(final MulticastSocket socket) {
        final SocketBindingManager.UnnamedBindingRegistry registry = this.manager.getUnnamedRegistry();
        registry.registerSocket(socket);
        return socket;
    }

    @Override
    public void close(Socket socket) throws IOException {
        final SocketBindingManager.UnnamedBindingRegistry registry = this.manager.getUnnamedRegistry();
        registry.unregisterSocket(socket);
        this.factory.close(socket);
    }

    @Override
    public void close(ServerSocket socket) throws IOException {
        final SocketBindingManager.UnnamedBindingRegistry registry = this.manager.getUnnamedRegistry();
        registry.unregisterSocket(socket);
        this.factory.close(socket);
    }

    @Override
    public void close(DatagramSocket socket) {
        final SocketBindingManager.UnnamedBindingRegistry registry = this.manager.getUnnamedRegistry();
        registry.unregisterSocket(socket);
        this.factory.close(socket);
    }

    @Override
    public Map<Object, String> getSockets() {
        return this.factory.getSockets();
    }
}
