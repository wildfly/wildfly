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

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.util.Util;

/**
 * Manages registration of all JGroups sockets with a {@link SocketBindingManager}.
 * @author Paul Ferraro
 */
public class ManagedSocketFactory implements SocketFactory {

    private final SocketBindingManager manager;
    // Maps a JGroups service name its associated SocketBinding
    private final Map<String, SocketBinding> bindings;
    // Store references to managed socket-binding registrations
    private final Map<SocketChannel, Closeable> channels = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<ServerSocketChannel, Closeable> serverChannels = Collections.synchronizedMap(new IdentityHashMap<>());

    public ManagedSocketFactory(SocketBindingManager manager, Map<String, SocketBinding> socketBindings) {
        this.manager = manager;
        this.bindings = socketBindings;
    }

    @Override
    public Socket createSocket(String name) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        org.jboss.as.network.ManagedSocketFactory factory = this.manager.getSocketFactory();
        return (binding != null) ? factory.createSocket(binding.getName()) : factory.createSocket();
    }

    @Override
    public ServerSocket createServerSocket(String name) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        org.jboss.as.network.ManagedServerSocketFactory factory = this.manager.getServerSocketFactory();
        return (binding != null) ? factory.createServerSocket(binding.getName()) : factory.createServerSocket();
    }

    @Override
    public DatagramSocket createDatagramSocket(String name, SocketAddress bindAddress) throws SocketException {
        SocketBinding binding = this.bindings.get(name);
        if (bindAddress == null) {
            // Creates unbound socket
            return (binding != null) ? this.manager.createDatagramSocket(binding.getName()) : this.manager.createDatagramSocket();
        }
        return (binding != null) ? this.manager.createDatagramSocket(binding.getName(), bindAddress) : this.manager.createDatagramSocket(bindAddress);
    }

    @Override
    public MulticastSocket createMulticastSocket(String name, SocketAddress bindAddress) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        if (bindAddress == null) {
            // Creates unbound socket
            return (binding != null) ? this.manager.createMulticastSocket(binding.getName()) : this.manager.createMulticastSocket();
        }
        return (binding != null) ? this.manager.createMulticastSocket(binding.getName(), bindAddress) : this.manager.createMulticastSocket(bindAddress);
    }

    @Override
    public SocketChannel createSocketChannel(String name) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        SocketChannel channel = SocketChannel.open();
        this.channels.put(channel, (binding != null) ? this.manager.getNamedRegistry().registerChannel(binding.getName(), channel) : this.manager.getUnnamedRegistry().registerChannel(channel));
        return channel;
    }

    @Override
    public ServerSocketChannel createServerSocketChannel(String name) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        ServerSocketChannel channel = ServerSocketChannel.open();
        this.serverChannels.put(channel, (binding != null) ? this.manager.getNamedRegistry().registerChannel(binding.getName(), channel) : this.manager.getUnnamedRegistry().registerChannel(channel));
        return channel;
    }

    @Override
    public void close(SocketChannel channel) {
        Util.close(this.channels.remove(channel));
        Util.close(channel);
    }

    @Override
    public void close(ServerSocketChannel channel) {
        Util.close(this.serverChannels.remove(channel));
        Util.close(channel);
    }
}
