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
    public Socket createSocket(String serviceName) throws IOException {
        return this.register(serviceName, this.factory.createSocket(serviceName));
    }

    @Override
    public Socket createSocket(String serviceName, String host, int port) throws IOException {
        return this.register(serviceName, this.factory.createSocket(serviceName, host, port));
    }

    @Override
    public Socket createSocket(String serviceName, InetAddress address, int port) throws IOException {
        return this.register(serviceName, this.factory.createSocket(serviceName, address, port));
    }

    @Override
    public Socket createSocket(String serviceName, String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return this.register(serviceName, this.factory.createSocket(serviceName, host, port, localAddress, localPort));
    }

    @Override
    public Socket createSocket(String serviceName, InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return this.register(serviceName, this.factory.createSocket(serviceName, address, port, localAddress, localPort));
    }

    private Socket register(final String name, final Socket socket) {
        final SocketBindingManager.NamedManagedBindingRegistry registry = this.manager.getNamedRegistry();
        registry.registerSocket(name, socket);
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(String serviceName) throws IOException {
        return this.register(serviceName, this.factory.createServerSocket(serviceName));
    }

    @Override
    public ServerSocket createServerSocket(String serviceName, int port) throws IOException {
        return this.register(serviceName, this.factory.createServerSocket(serviceName, port));
    }

    @Override
    public ServerSocket createServerSocket(String serviceName, int port, int backlog) throws IOException {
        return this.register(serviceName, this.factory.createServerSocket(serviceName, port, backlog));
    }

    @Override
    public ServerSocket createServerSocket(String serviceName, int port, int backlog, InetAddress bindAddr) throws IOException {
        return this.register(serviceName, this.factory.createServerSocket(serviceName, port, backlog, bindAddr));
    }

    private ServerSocket register(final String name, ServerSocket socket) {
        final SocketBindingManager.NamedManagedBindingRegistry registry = this.manager.getNamedRegistry();
        registry.registerSocket(name, socket);
        return socket;
    }

    @Override
    public DatagramSocket createDatagramSocket(String serviceName) throws SocketException {
        return this.register(serviceName, this.factory.createDatagramSocket(serviceName));
    }

    @Override
    public DatagramSocket createDatagramSocket(String serviceName, SocketAddress bindAddress) throws SocketException {
        return this.register(serviceName, this.factory.createDatagramSocket(serviceName, bindAddress));
    }

    @Override
    public DatagramSocket createDatagramSocket(String serviceName, int port) throws SocketException {
        return this.register(serviceName, this.factory.createDatagramSocket(serviceName, port));
    }

    @Override
    public DatagramSocket createDatagramSocket(String serviceName, int port, InetAddress localAddress) throws SocketException {
        return this.register(serviceName, this.factory.createDatagramSocket(serviceName, port, localAddress));
    }

    private DatagramSocket register(final String name, final DatagramSocket socket) {
        final SocketBindingManager.NamedManagedBindingRegistry registry = this.manager.getNamedRegistry();
        registry.registerSocket(name, socket);
        return socket;
    }

    @Override
    public MulticastSocket createMulticastSocket(String serviceName) throws IOException {
        return this.register(serviceName, this.factory.createMulticastSocket(serviceName));
    }

    @Override
    public MulticastSocket createMulticastSocket(String serviceName, int port) throws IOException {
        return this.register(serviceName, this.factory.createMulticastSocket(serviceName, port));
    }

    @Override
    public MulticastSocket createMulticastSocket(String serviceName, SocketAddress bindAddress) throws IOException {
        return this.register(serviceName, this.factory.createMulticastSocket(serviceName, bindAddress));
    }

    private MulticastSocket register(final String name, final MulticastSocket socket) {
        final SocketBindingManager.NamedManagedBindingRegistry registry = this.manager.getNamedRegistry();
        registry.registerSocket(name, socket);
        return socket;
    }

    @Override
    public void close(Socket sock) throws IOException {
        final SocketBindingManager.NamedManagedBindingRegistry registry = this.manager.getNamedRegistry();
        final String name = getSockets().get(sock);
        registry.unregisterBinding(name);
        this.factory.close(sock);
    }

    @Override
    public void close(ServerSocket sock) throws IOException {
        final SocketBindingManager.NamedManagedBindingRegistry registry = this.manager.getNamedRegistry();
        final String name = getSockets().get(sock);
        registry.unregisterBinding(name);
        this.factory.close(sock);
    }

    @Override
    public void close(DatagramSocket sock) {
        final SocketBindingManager.NamedManagedBindingRegistry registry = this.manager.getNamedRegistry();
        final String name = getSockets().get(sock);
        registry.unregisterBinding(name);
        this.factory.close(sock);
    }

    @Override
    public Map<Object, String> getSockets() {
        return this.factory.getSockets();
    }
}
