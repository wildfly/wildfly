/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.util.Util;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Manages registration of all JGroups sockets with a {@link SocketBindingManager}.
 * @author Paul Ferraro
 */
public class ManagedSocketFactory implements SocketFactory {

    private final SelectorProvider provider;
    protected final SocketBindingManager manager;
    // Maps a JGroups service name its associated SocketBinding
    protected final Map<String, SocketBinding> bindings;
    // Store references to managed socket-binding registrations
    protected final Map<Object, Closeable> closeables = Collections.synchronizedMap(new IdentityHashMap<>());

    public ManagedSocketFactory(SelectorProvider provider, SocketBindingManager manager, Map<String, SocketBinding> socketBindings) {
        this.provider = provider;
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
        return this.createNetworkChannel(name, SelectorProvider::openSocketChannel, SocketBindingManager.NamedManagedBindingRegistry::registerChannel, SocketBindingManager.UnnamedBindingRegistry::registerChannel);
    }

    @Override
    public ServerSocketChannel createServerSocketChannel(String name) throws IOException {
        return this.createNetworkChannel(name, SelectorProvider::openServerSocketChannel, SocketBindingManager.NamedManagedBindingRegistry::registerChannel, SocketBindingManager.UnnamedBindingRegistry::registerChannel);
    }

    @Override
    public void close(SocketChannel channel) {
        this.closeNetworkChannel(channel);
    }

    @Override
    public void close(ServerSocketChannel channel) {
        this.closeNetworkChannel(channel);
    }

    private <C extends NetworkChannel> C createNetworkChannel(String name, ExceptionFunction<SelectorProvider, C, IOException> factory, TriFunction<SocketBindingManager.NamedManagedBindingRegistry, String, C, Closeable> namedRegistration, BiFunction<SocketBindingManager.UnnamedBindingRegistry, C, Closeable> unnamedRegistration) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        C channel = factory.apply(this.provider);
        this.closeables.put(channel, (binding != null) ? namedRegistration.apply(this.manager.getNamedRegistry(), binding.getName(), channel) : unnamedRegistration.apply(this.manager.getUnnamedRegistry(), channel));
        return channel;
    }

    private void closeNetworkChannel(NetworkChannel channel) {
        Util.close(this.closeables.remove(channel));
        Util.close(channel);
    }

    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
