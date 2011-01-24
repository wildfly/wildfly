/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.server.services.net;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingManagerService implements SocketBindingManager, Service<SocketBindingManager> {

    private final InjectedValue<NetworkInterfaceBinding> defaultInterfaceBinding = new InjectedValue<NetworkInterfaceBinding>();
    private final int portOffSet;
    private final SocketFactory socketFactory = new ManagedSocketFactory();
    private final ServerSocketFactory serverSocketFactory = new ManagedServerSocketFactory();

    private final Map<InetSocketAddress, ManagedBinding> managedBindings = new ConcurrentHashMap<InetSocketAddress, ManagedBinding>();

    public SocketBindingManagerService(int portOffSet) {
        this.portOffSet = portOffSet;
    }

    public InjectedValue<NetworkInterfaceBinding> getDefaultInterfaceBinding() {
        return defaultInterfaceBinding;
    }

    /**
     * Return the resolved {@link InetAddress} for the default interface.
     *
     * @return the resolve address
     */
    public InetAddress getDefaultInterfaceAddress() {
        return defaultInterfaceBinding.getValue().getAddress();
    }

    @Override
    public void start(StartContext context) throws StartException {
        //
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public SocketBindingManager getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public int getPortOffset() {
        return portOffSet;
    }

    /**
     * Get the managed server socket factory.
     *
     * @return the server socket factory
     */
    @Override
    public ServerSocketFactory getServerSocketFactory() {
        return serverSocketFactory;
    }

    /**
     * Get the socket factory.
     *
     * @return the socket factory
     */
    @Override
    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    /**
     * Create a datagram socket.
     *
     * @param address the socket address
     * @return the datagram socket
     * @throws SocketException
     */
    @Override
    public DatagramSocket createDatagramSocket(SocketAddress address) throws SocketException {
        return new ManagedDatagramSocketBinding(this, address);
    }

    /**
     * Create a multicast socket.
     *
     * @param address the socket address
     * @return the multicast socket
     * @throws IOException
     */
    @Override
    public MulticastSocket createMulticastSocket(SocketAddress address) throws IOException {
        return new ManagedMulticastSocketBinding(this, address);
    }

    /**
     * @return the registered bindings
     */
    @Override
    public Collection<ManagedBinding> listActiveBindings() {
        return managedBindings.values();
    }

    /**
     * Register an active socket binding.
     *
     * @param binding the managed binding
     * @param bindingName the binding name
     */
    @Override
    public Closeable registerBinding(ManagedBinding binding) {
        managedBindings.put(binding.getBindAddress(), binding);
        return binding;
    }

    @Override
    public Closeable registerSocket(DatagramSocket socket) {
        return registerBinding(new WrappedManagedDatagramSocket(socket));
    }
    @Override
    public Closeable registerSocket(ServerSocket socket) {
        return registerBinding(new WrappedManagedServerSocket(socket));
    }
    @Override
    public Closeable registerSocket(Socket socket) {
        return registerBinding(new WrappedManagedSocket(socket));
    }
    @Override
    public Closeable registerChannel(DatagramChannel channel) {
        return registerBinding((InetSocketAddress) channel.socket().getLocalSocketAddress(), channel);
    }
    @Override
    public Closeable registerChannel(ServerSocketChannel channel) {
        return registerBinding((InetSocketAddress) channel.socket().getLocalSocketAddress(), channel);
    }
    @Override
    public Closeable registerChannel(SocketChannel channel) {
        return registerBinding((InetSocketAddress) channel.socket().getLocalSocketAddress(), channel);
    }

    /**
     * Unregister a socket binding.
     *
     * @param binding the managed socket binding
     */
    @Override
    public void unregisterBinding(ManagedBinding binding) {
        unregisterBinding(binding.getBindAddress());
    }

    @Override
    public void unregisterSocket(DatagramSocket socket) {
        unregisterBinding((InetSocketAddress) socket.getLocalSocketAddress());
    }
    @Override
    public void unregisterSocket(ServerSocket socket) {
        unregisterBinding((InetSocketAddress) socket.getLocalSocketAddress());
    }
    @Override
    public void unregisterSocket(Socket socket) {
        unregisterBinding((InetSocketAddress) socket.getLocalSocketAddress());
    }
    @Override
    public void unregisterChannel(DatagramChannel channel) {
        unregisterBinding((InetSocketAddress) channel.socket().getLocalSocketAddress());
    }
    @Override
    public void unregisterChannel(ServerSocketChannel channel) {
        unregisterBinding((InetSocketAddress) channel.socket().getLocalSocketAddress());
    }
    @Override
    public void unregisterChannel(SocketChannel channel) {
        unregisterBinding((InetSocketAddress) channel.socket().getLocalSocketAddress());
    }

    Closeable registerBinding(InetSocketAddress address, Closeable closeable) {
        return registerBinding(new CloseableManagedBinding(address, closeable));
    }

    void unregisterBinding(InetSocketAddress address) {
        managedBindings.remove(address);
    }

    class ManagedSocketFactory extends SocketFactory {

        @Override
        public Socket createSocket() {
            return new ManagedSocketBinding(SocketBindingManagerService.this);
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
    }

    class ManagedServerSocketFactory extends ServerSocketFactory {

        @Override
        public ServerSocket createServerSocket() throws IOException {
            return new ManagedServerSocketBinding(SocketBindingManagerService.this);
        }

        @Override
        public ServerSocket createServerSocket(final int port) throws IOException {
            final ServerSocket serverSocket = createServerSocket();
            serverSocket.bind(new InetSocketAddress(port));
            return serverSocket;
        }

        @Override
        public ServerSocket createServerSocket(final int port, final int backlog) throws IOException {
            final ServerSocket serverSocket = createServerSocket();
            serverSocket.bind(new InetSocketAddress(port), backlog);
            return serverSocket;
        }

        @Override
        public ServerSocket createServerSocket(final int port, final int backlog, final InetAddress ifAddress) throws IOException {
            final ServerSocket serverSocket = createServerSocket();
            serverSocket.bind(new InetSocketAddress(ifAddress, port), backlog);
            return serverSocket;
        }
    }

    class CloseableManagedBinding implements ManagedBinding {
        private final InetSocketAddress address;
        private final Closeable closeable;
        public CloseableManagedBinding(final InetSocketAddress address, final Closeable closeable) {
            this.address = address;
            this.closeable = closeable;
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
                unregisterBinding(address);
            }
        }
    }

    class WrappedManagedDatagramSocket implements ManagedBinding {
        private final DatagramSocket socket;
        public WrappedManagedDatagramSocket(final DatagramSocket socket) {
            this.socket = socket;
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
                unregisterBinding(getBindAddress());
            }
        }
    }

    class WrappedManagedSocket implements ManagedBinding {
        private final Socket socket;
        public WrappedManagedSocket(final Socket socket) {
            this.socket = socket;
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
                unregisterBinding(getBindAddress());
            }
        }
    }

    class WrappedManagedServerSocket implements ManagedBinding {
        private final ServerSocket socket;
        public WrappedManagedServerSocket(final ServerSocket socket) {
            this.socket = socket;
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
                unregisterBinding(getBindAddress());
            }
        }
    }

}

