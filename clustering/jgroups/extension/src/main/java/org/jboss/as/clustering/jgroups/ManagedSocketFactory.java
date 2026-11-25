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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.jgroups.spi.TLSConfiguration;

/**
 * Manages registration of all JGroups sockets with a {@link SocketBindingManager}.
 * @author Paul Ferraro
 */
public class ManagedSocketFactory implements SocketFactory {

    interface Configuration extends TLSConfiguration {
        SocketBindingManager getSocketBindingManager();
        Map<String, SocketBinding> getSocketBindings();

        default SelectorProvider getSelectorProvider() {
            return SelectorProvider.provider();
        }
    }

    private final SelectorProvider provider;
    private final SocketBindingManager manager;
    // Maps a JGroups service name its associated SocketBinding
    private final Map<String, SocketBinding> bindings;
    // Manually created socket-binding registrations per socket/channel
    private final Map<Closeable, Closeable> registrations = Collections.synchronizedMap(new IdentityHashMap<>());
    private final SSLContext clientSSLContext;
    private final SSLContext serverSSLContext;
    private final Consumer<? super Closeable> closeTask;

    public ManagedSocketFactory(Configuration configuration) {
        this.provider = configuration.getSelectorProvider();
        this.manager = configuration.getSocketBindingManager();
        this.bindings = configuration.getSocketBindings();
        this.clientSSLContext = configuration.getClientSSLContext();
        this.serverSSLContext = configuration.getServerSSLContext();
        // Close any SocketBindingManager registration before closing the socket/channel
        this.closeTask = Consumer.close().<Closeable>compose(this.registrations::remove).andThen(Consumer.close());
    }

    @Override
    public Socket createSocket(String name) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        Socket socket = (this.clientSSLContext != null) ? this.clientSSLContext.getSocketFactory().createSocket() : (binding != null) ? this.manager.getSocketFactory().createSocket(binding.getName()) : this.manager.getSocketFactory().createSocket();
        if (this.clientSSLContext != null) {
            // If Socket was not created by SocketBindingManager, register manually
            this.registrations.put(socket, (binding != null) ? this.manager.getNamedRegistry().registerSocket(binding.getName(), socket) : this.manager.getUnnamedRegistry().registerSocket(socket));
        }
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(String name) throws IOException {
        SocketBinding binding = this.bindings.get(name);
        ServerSocket socket = (this.serverSSLContext != null) ? this.serverSSLContext.getServerSocketFactory().createServerSocket() : (binding != null) ? this.manager.getServerSocketFactory().createServerSocket(binding.getName()) : this.manager.getServerSocketFactory().createServerSocket();
        if (this.serverSSLContext != null) {
            // If ServerSocket was not created by SocketBindingManager, register manually
            this.registrations.put(socket, (binding != null) ? this.manager.getNamedRegistry().registerSocket(binding.getName(), socket) : this.manager.getUnnamedRegistry().registerSocket(socket));
        }
        return socket;
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
        if (this.clientSSLContext != null) {
            if (name != null) {
                JGroupsLogger.ROOT_LOGGER.secureSocketChannelNotAvailable(name);
            } else {
                JGroupsLogger.ROOT_LOGGER.secureSocketChannelNotAvailable();
            }
        }
        SocketBinding binding = this.bindings.get(name);
        SocketChannel channel = this.provider.openSocketChannel();
        this.registrations.put(channel, (binding != null) ? this.manager.getNamedRegistry().registerChannel(binding.getName(), channel) : this.manager.getUnnamedRegistry().registerChannel(channel));
        return channel;
    }

    @Override
    public ServerSocketChannel createServerSocketChannel(String name) throws IOException {
        if (this.serverSSLContext != null) {
            if (name != null) {
                JGroupsLogger.ROOT_LOGGER.secureSocketChannelNotAvailable(name);
            } else {
                JGroupsLogger.ROOT_LOGGER.secureSocketChannelNotAvailable();
            }
        }
        SocketBinding binding = this.bindings.get(name);
        ServerSocketChannel channel = this.provider.openServerSocketChannel();
        this.registrations.put(channel, (binding != null) ? this.manager.getNamedRegistry().registerChannel(binding.getName(), channel) : this.manager.getUnnamedRegistry().registerChannel(channel));
        return channel;
    }

    @Override
    public void close(Socket socket) throws IOException {
        this.closeTask.accept(socket);
    }

    @Override
    public void close(ServerSocket socket) throws IOException {
        this.closeTask.accept(socket);
    }

    @Override
    public void close(SocketChannel channel) {
        this.closeTask.accept(channel);
    }

    @Override
    public void close(ServerSocketChannel channel) {
        this.closeTask.accept(channel);
    }
}
