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

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Emanuel Muckenhuber
 */
public abstract class SocketBindingManagerImpl implements SocketBindingManager {

    private final ManagedSocketFactory socketFactory = new ManagedSocketFactoryImpl();
    private final ManagedServerSocketFactory serverSocketFactory = new ManagedServerSocketFactoryImpl();

    private final NamedManagedBindingRegistry namedRegistry = new NamedRegistryImpl();
    private final UnnamedBindingRegistry unnamedRegistry = new UnnamedRegistryImpl();

    /** {@inheritDoc} */
    @Override
    public ManagedServerSocketFactory getServerSocketFactory() {
        return serverSocketFactory;
    }

    /** {@inheritDoc} */
    @Override
    public ManagedSocketFactory getSocketFactory() {
        return socketFactory;
    }

    /** {@inheritDoc} */
    @Override
    public DatagramSocket createDatagramSocket(String name, SocketAddress address) throws SocketException {
        return new ManagedDatagramSocketBinding(null, this.namedRegistry, address);
    }

    /** {@inheritDoc} */
    @Override
    public DatagramSocket createDatagramSocket(SocketAddress address) throws SocketException {
        return new ManagedDatagramSocketBinding(null, this.unnamedRegistry, address);
    }

    /** {@inheritDoc} */
    @Override
    public MulticastSocket createMulticastSocket(String name, SocketAddress address) throws IOException {
        return ManagedMulticastSocketBinding.create(null, this.unnamedRegistry, address);
    }

    /** {@inheritDoc} */
    @Override
    public MulticastSocket createMulticastSocket(SocketAddress address) throws IOException {
        return ManagedMulticastSocketBinding.create(null, this.unnamedRegistry, address);
    }

    /** {@inheritDoc} */
    @Override
    public NamedManagedBindingRegistry getNamedRegistry() {
        return namedRegistry;
    }

    /** {@inheritDoc} */
    @Override
    public UnnamedBindingRegistry getUnnamedRegistry() {
        return unnamedRegistry;
    }

    class ManagedSocketFactoryImpl extends ManagedSocketFactory {

        @Override
        public Socket createSocket() {
            return new ManagedSocketBinding(SocketBindingManagerImpl.this.unnamedRegistry);
        }

        @Override
        public Socket createSocket(final String host, final int port) throws IOException {
            return createSocket(InetAddress.getByName(host), port);
        }

        @Override
        public Socket createSocket(final InetAddress host, final int port) throws IOException {
            final Socket socket = createSocket();
            socket.connect(new InetSocketAddress(host, port));
            return socket;
        }

        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localHost, final int localPort) throws IOException {
            return createSocket(InetAddress.getByName(host), port, localHost, localPort);
        }

        @Override
        public Socket createSocket(final InetAddress address, final int port, final InetAddress localAddress, final int localPort) throws IOException {
            final Socket socket = createSocket();
            socket.bind(new InetSocketAddress(localAddress, localPort));
            socket.connect(new InetSocketAddress(address, port));
            return socket;
        }

        @Override
        public Socket createSocket(final String name) {
            return new ManagedSocketBinding(name, SocketBindingManagerImpl.this.namedRegistry);
        }

        @Override
        public Socket createSocket(final String name, final String host, final int port) throws IOException {
            return createSocket(name, InetAddress.getByName(host), port);
        }

        @Override
        public Socket createSocket(final String name, final InetAddress host, final int port) throws IOException {
            final Socket socket = createSocket(name);
            socket.connect(new InetSocketAddress(host, port));
            return socket;
        }

        @Override
        public Socket createSocket(final String name, final String host, final int port, final InetAddress localHost, final int localPort) throws IOException {
            return createSocket(name, InetAddress.getByName(host), port, localHost, localPort);
        }

        @Override
        public Socket createSocket(final String name, final InetAddress address, final int port, final InetAddress localAddress, final int localPort) throws IOException {
            final Socket socket = createSocket(name);
            socket.bind(new InetSocketAddress(localAddress, localPort));
            socket.connect(new InetSocketAddress(address, port));
            return socket;
        }

    }

    class ManagedServerSocketFactoryImpl extends ManagedServerSocketFactory {

        @Override
        public ServerSocket createServerSocket(String name) throws IOException {
            return new ManagedServerSocketBinding(name, SocketBindingManagerImpl.this);
        }
        @Override
        public ServerSocket createServerSocket() throws IOException {
            return createServerSocket(null);
        }
        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            return createServerSocket(null, port);
        }

        @Override
        public ServerSocket createServerSocket(String name, final int port) throws IOException {
            final ServerSocket serverSocket = createServerSocket(name);
            serverSocket.bind(new InetSocketAddress(port));
            return serverSocket;
        }

        @Override
        public ServerSocket createServerSocket(final int port, final int backlog) throws IOException {
            return createServerSocket(null, port, backlog);
        }

        @Override
        public ServerSocket createServerSocket(String name, int port, int backlog) throws IOException {
            final ServerSocket serverSocket = createServerSocket(name);
            serverSocket.bind(new InetSocketAddress(port), backlog);
            return serverSocket;
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
            return createServerSocket(null, port, backlog, ifAddress);
        }

        @Override
        public ServerSocket createServerSocket(final String name, final int port, final int backlog, final InetAddress ifAddress) throws IOException {
            final ServerSocket serverSocket = createServerSocket(name);
            serverSocket.bind(new InetSocketAddress(ifAddress, port), backlog);
            return serverSocket;
        }

    }

    static class CloseableManagedBinding implements ManagedBinding {
        private final String name;
        private final InetSocketAddress address;
        private final Closeable closeable;
        private final ManagedBindingRegistry registry;
        CloseableManagedBinding(final InetSocketAddress address, final Closeable closeable, final ManagedBindingRegistry registry) {
            this(null, address, closeable, registry);
        }
        CloseableManagedBinding(final String name, final InetSocketAddress address,
                final Closeable closeable, final ManagedBindingRegistry registry) {
            this.name = name;
            this.address = address;
            this.closeable = closeable;
            this.registry = registry;
        }
        @Override
        public String getSocketBindingName() {
            return name;
        }
        @Override
        public InetSocketAddress getBindAddress() {
            return address;
        }
        @Override
        public void close() throws IOException {
            try {
                closeable.close();
            } finally {
                registry.unregisterBinding(this);
            }
        }
    }

    static class WrappedManagedDatagramSocket implements ManagedBinding {
        private final String name;
        private final DatagramSocket socket;
        private final ManagedBindingRegistry registry;
        public WrappedManagedDatagramSocket(final DatagramSocket socket, final ManagedBindingRegistry registry) {
            this(null, socket, registry);
        }
        public WrappedManagedDatagramSocket(final String name, final DatagramSocket socket, final ManagedBindingRegistry registry) {
            this.name = name;
            this.socket = socket;
            this.registry = registry;
        }
        /** {@inheritDoc} */
        @Override
        public String getSocketBindingName() {
            return name;
        }
        @Override
        public InetSocketAddress getBindAddress() {
            return (InetSocketAddress) socket.getLocalSocketAddress();
        }
        @Override
        public void close() throws IOException {
            try {
                socket.close();
            } finally {
                registry.unregisterBinding(this);
            }
        }
    }

    static class WrappedManagedBinding implements ManagedBinding {
        private final ManagedBinding wrapped;
        private final ManagedBindingRegistry registry;
        public WrappedManagedBinding(final ManagedBinding wrapped, final ManagedBindingRegistry registry) {
            this.wrapped = wrapped;
            this.registry = registry;
        }
        @Override
        public String getSocketBindingName() {
            return wrapped.getSocketBindingName();
        }
        @Override
        public InetSocketAddress getBindAddress() {
            return wrapped.getBindAddress();
        }
        @Override
        public void close() throws IOException {
            try {
                wrapped.close();
            } finally {
                registry.unregisterBinding(this);
            }
        }
    }

    static class WrappedManagedSocket implements ManagedBinding {
        private final String name;
        private final Socket socket;
        private final ManagedBindingRegistry registry;
        public WrappedManagedSocket(final Socket socket, final ManagedBindingRegistry registry) {
            this(null, socket, registry);
        }
        public WrappedManagedSocket(final String name, final Socket socket, final ManagedBindingRegistry registry) {
            this.name = name;
            this.socket = socket;
            this.registry = registry;
        }
        @Override
        public String getSocketBindingName() {
            return name;
        }
        @Override
        public InetSocketAddress getBindAddress() {
            return (InetSocketAddress) socket.getLocalSocketAddress();
        }
        @Override
        public void close() throws IOException {
            try {
                socket.close();
            } finally {
                registry.unregisterBinding(this);
            }
        }
    }

    static class WrappedManagedServerSocket implements ManagedBinding {
        private final String name;
        private final ServerSocket socket;
        private final ManagedBindingRegistry registry;
        public WrappedManagedServerSocket(final ServerSocket socket, final ManagedBindingRegistry registry) {
            this(null, socket, registry);
        }
        public WrappedManagedServerSocket(final String name, final ServerSocket socket, final ManagedBindingRegistry registry) {
            this.name = name;
            this.socket = socket;
            this.registry = registry;
        }
        @Override
        public String getSocketBindingName() {
            return name;
        }
        @Override
        public InetSocketAddress getBindAddress() {
            return (InetSocketAddress) socket.getLocalSocketAddress();
        }
        @Override
        public void close() throws IOException {
            try {
                socket.close();
            } finally {
                registry.unregisterBinding(this);
            }
        }
    }

    static final class NamedRegistryImpl implements NamedManagedBindingRegistry {
        private final Map<String, ManagedBinding> bindings = new ConcurrentHashMap<String, ManagedBinding>();

        /** {@inheritDoc} */
        @Override
        public ManagedBinding getManagedBinding(String name) {
            return bindings.get(name);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isRegistered(String name) {
            return bindings.containsKey(name);
        }

        /** {@inheritDoc} */
        @Override
        public void registerBinding(ManagedBinding binding) {
            final String name = binding.getSocketBindingName();
            if(name == null) {
                throw new IllegalStateException();
            }
            bindings.put(name, new WrappedManagedBinding(binding, this));
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterBinding(ManagedBinding binding) {
            final String name = binding.getSocketBindingName();
            if(name == null) {
                throw new IllegalStateException();
            }
            unregisterBinding(name);
        }

        /** {@inheritDoc} */
        @Override
        public Collection<ManagedBinding> listActiveBindings() {
            return new HashSet<ManagedBinding>(bindings.values());
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerSocket(String name, Socket socket) {
            final ManagedBinding binding = new WrappedManagedSocket(name, socket, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerSocket(String name, ServerSocket socket) {
            final ManagedBinding binding = new WrappedManagedServerSocket(name, socket, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerSocket(String name, DatagramSocket socket) {
            final ManagedBinding binding = new WrappedManagedDatagramSocket(name, socket, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerChannel(String name, SocketChannel channel) {
            final ManagedBinding binding = new CloseableManagedBinding(name, (InetSocketAddress) channel.socket().getLocalSocketAddress(), channel, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerChannel(String name, ServerSocketChannel channel) {
            final ManagedBinding binding = new CloseableManagedBinding(name, (InetSocketAddress) channel.socket().getLocalSocketAddress(), channel, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerChannel(String name, DatagramChannel channel) {
            final ManagedBinding binding = new CloseableManagedBinding(name, (InetSocketAddress) channel.socket().getLocalSocketAddress(), channel, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterBinding(String name) {
            if(name == null) {
                return;
            }
            bindings.remove(name);
        }
    }

    static final class UnnamedRegistryImpl implements UnnamedBindingRegistry {
        private final Map<InetSocketAddress, ManagedBinding> bindings = new ConcurrentHashMap<InetSocketAddress, ManagedBinding>();

        /** {@inheritDoc} */
        @Override
        public void registerBinding(ManagedBinding binding) {
            final InetSocketAddress address = binding.getBindAddress();
            if(address == null) {
                throw new IllegalStateException();
            }
            bindings.put(address, new WrappedManagedBinding(binding, this));
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterBinding(ManagedBinding binding) {
            final InetSocketAddress address = binding.getBindAddress();
            if(address == null) {
                throw new IllegalStateException();
            }
            unregisterBinding(address);
        }

        /** {@inheritDoc} */
        @Override
        public Collection<ManagedBinding> listActiveBindings() {
            return new HashSet<ManagedBinding>(bindings.values());
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerSocket(Socket socket) {
            final ManagedBinding binding = new WrappedManagedSocket(socket, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerSocket(ServerSocket socket) {
            final ManagedBinding binding = new WrappedManagedServerSocket(socket, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerSocket(DatagramSocket socket) {
            final ManagedBinding binding = new WrappedManagedDatagramSocket(socket, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerChannel(SocketChannel channel) {
            final ManagedBinding binding = new CloseableManagedBinding((InetSocketAddress) channel.socket().getLocalSocketAddress(), channel, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerChannel(ServerSocketChannel channel) {
            final ManagedBinding binding = new CloseableManagedBinding((InetSocketAddress) channel.socket().getLocalSocketAddress(), channel, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public Closeable registerChannel(DatagramChannel channel) {
            final ManagedBinding binding = new CloseableManagedBinding((InetSocketAddress) channel.socket().getLocalSocketAddress(), channel, this);
            registerBinding(binding);
            return binding;
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterSocket(Socket socket) {
            unregisterBinding((InetSocketAddress) socket.getLocalSocketAddress());
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterSocket(ServerSocket socket) {
            unregisterBinding((InetSocketAddress) socket.getLocalSocketAddress());
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterSocket(DatagramSocket socket) {
            unregisterBinding((InetSocketAddress) socket.getLocalSocketAddress());
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterChannel(SocketChannel channel) {
            unregisterBinding((InetSocketAddress) channel.socket().getLocalSocketAddress());
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterChannel(ServerSocketChannel channel) {
            unregisterBinding((InetSocketAddress) channel.socket().getLocalSocketAddress());
        }

        /** {@inheritDoc} */
        @Override
        public void unregisterChannel(DatagramChannel channel) {
            unregisterBinding((InetSocketAddress) channel.socket().getLocalSocketAddress());
        }

        public void unregisterBinding(final InetSocketAddress address) {
            if(address != null) {
                bindings.remove(address);
            }
        }
    }

}
