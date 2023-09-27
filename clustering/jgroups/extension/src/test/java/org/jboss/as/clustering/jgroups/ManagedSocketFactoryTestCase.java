/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.Map;

import org.jboss.as.network.ManagedServerSocketFactory;
import org.jboss.as.network.ManagedSocketFactory;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.network.SocketBindingManager.NamedManagedBindingRegistry;
import org.jboss.as.network.SocketBindingManager.UnnamedBindingRegistry;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

/**
 * @author Paul Ferraro
 */
public class ManagedSocketFactoryTestCase {

    private final SocketBindingManager manager = mock(SocketBindingManager.class);
    private final SelectorProvider provider = mock(SelectorProvider.class);

    private final SocketFactory subject = new org.jboss.as.clustering.jgroups.ManagedSocketFactory(this.provider, this.manager, Map.of("known-service", new SocketBinding("binding", 0, false, null, 0, null, this.manager, List.of())));

    @Test
    public void createSocket() throws IOException {
        this.createSocket("known-service", "binding");
        this.createSocket("unknown-service", null);
    }

    private void createSocket(String serviceName, String bindingName) throws IOException {
        ManagedSocketFactory factory = mock(ManagedSocketFactory.class);
        Socket socket = mock(Socket.class);

        when(this.manager.getSocketFactory()).thenReturn(factory);
        if (bindingName != null) {
            when(factory.createSocket(bindingName)).thenReturn(socket);
        } else {
            when(factory.createSocket()).thenReturn(socket);
        }

        try (Socket result = this.subject.createSocket(serviceName)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());
            verify(socket, never()).connect(any());
        }
        reset(socket);

        InetAddress connectAddress = InetAddress.getLocalHost();
        int connectPort = 1;

        try (Socket result = this.subject.createSocket(serviceName, connectAddress, connectPort)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
        reset(socket);

        try (Socket result = this.subject.createSocket(serviceName, connectAddress.getHostName(), connectPort)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
        reset(socket);

        InetAddress bindAddress = InetAddress.getLoopbackAddress();
        int bindPort = 2;

        try (Socket result = this.subject.createSocket(serviceName, connectAddress, connectPort, bindAddress, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedBindAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedBindAddress.capture());
            InetSocketAddress bindSocketAddress = capturedBindAddress.getValue();
            assertEquals(bindAddress, bindSocketAddress.getAddress());
            assertEquals(bindPort, bindSocketAddress.getPort());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
        reset(socket);

        try (Socket result = this.subject.createSocket(serviceName, connectAddress.getHostName(), connectPort, bindAddress, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedBindAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedBindAddress.capture());
            InetSocketAddress bindSocketAddress = capturedBindAddress.getValue();
            assertEquals(bindAddress, bindSocketAddress.getAddress());
            assertEquals(bindPort, bindSocketAddress.getPort());

            ArgumentCaptor<InetSocketAddress> capturedConnectAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).connect(capturedConnectAddress.capture());
            InetSocketAddress connectSocketAddress = capturedConnectAddress.getValue();
            assertEquals(connectAddress, connectSocketAddress.getAddress());
            assertEquals(connectPort, connectSocketAddress.getPort());
        }
    }

    @Test
    public void createServerSocket() throws IOException {
        this.createServerSocket("known-service", "binding");
        this.createServerSocket("unknown-service", null);
    }

    private void createServerSocket(String serviceName, String bindingName) throws IOException {
        ManagedServerSocketFactory factory = mock(ManagedServerSocketFactory.class);
        ServerSocket socket = mock(ServerSocket.class);

        when(this.manager.getServerSocketFactory()).thenReturn(factory);
        if (bindingName != null) {
            when(factory.createServerSocket(bindingName)).thenReturn(socket);
        } else {
            when(factory.createServerSocket()).thenReturn(socket);
        }

        try (ServerSocket result = this.subject.createServerSocket(serviceName)) {
            assertSame(socket, result);

            verify(socket, never()).bind(any());
        }
        reset(socket);

        int bindPort = 1;

        try (ServerSocket result = this.subject.createServerSocket(serviceName, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedAddress.capture(), eq(SocketFactory.DEFAULT_BACKLOG));
            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket);

        int backlog = 10;

        try (ServerSocket result = this.subject.createServerSocket(serviceName, bindPort, backlog)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedAddress.capture(), eq(backlog));
            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket);

        InetAddress bindAddress = InetAddress.getLoopbackAddress();

        try (ServerSocket result = this.subject.createServerSocket(serviceName, bindPort, backlog, bindAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            verify(socket).bind(capturedAddress.capture(), eq(backlog));
            InetSocketAddress address = capturedAddress.getValue();
            assertEquals(bindAddress, address.getAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket);
    }

    @Test
    public void createDatagramSocket() throws IOException {
        this.createDatagramSocket("known-service", "binding");
        this.createDatagramSocket("unknown-service", null);
    }

    private void createDatagramSocket(String serviceName, String bindingName) throws IOException {
        DatagramSocket socket = mock(DatagramSocket.class);

        if (bindingName != null) {
            when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createDatagramSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createDatagramSocket(capturedAddress.capture());
            }

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(0, address.getPort());
        }
        reset(socket, this.manager);

        int bindPort = 1;
        if (bindingName != null) {
            when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createDatagramSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createDatagramSocket(capturedAddress.capture());
            }

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        InetAddress bindAddress = InetAddress.getLocalHost();
        if (bindingName != null) {
            when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createDatagramSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, bindPort, bindAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createDatagramSocket(capturedAddress.capture());
            }

            InetSocketAddress address = capturedAddress.getValue();
            assertSame(bindAddress, address.getAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        if (bindingName != null) {
            when(this.manager.createDatagramSocket(eq(bindingName))).thenReturn(socket);
        } else {
            when(this.manager.createDatagramSocket()).thenReturn(socket);
        }

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, null)) {
            assertSame(socket, result);
        }
        reset(socket, this.manager);

        SocketAddress socketAddress = new InetSocketAddress(bindAddress, bindPort);
        if (bindingName != null) {
            when(this.manager.createDatagramSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createDatagramSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (DatagramSocket result = this.subject.createDatagramSocket(serviceName, socketAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<SocketAddress> capturedAddress = ArgumentCaptor.forClass(SocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createDatagramSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createDatagramSocket(capturedAddress.capture());
            }

            assertSame(socketAddress, capturedAddress.getValue());
        }
    }

    @Test
    public void createMulticastSocket() throws IOException {
        this.createMulticastSocket("known-service", "binding");
        this.createMulticastSocket("unknown-service", null);
    }

    private void createMulticastSocket(String serviceName, String bindingName) throws IOException {
        MulticastSocket socket = mock(MulticastSocket.class);

        if (bindingName != null) {
            when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createMulticastSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createMulticastSocket(capturedAddress.capture());
            }

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(0, address.getPort());
        }
        reset(socket, this.manager);

        int bindPort = 1;
        if (bindingName != null) {
            when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createMulticastSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, bindPort)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createMulticastSocket(capturedAddress.capture());
            }

            InetSocketAddress address = capturedAddress.getValue();
            assertTrue(address.getAddress().isAnyLocalAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        InetAddress bindAddress = InetAddress.getLocalHost();
        if (bindingName != null) {
            when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createMulticastSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, bindPort, bindAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<InetSocketAddress> capturedAddress = ArgumentCaptor.forClass(InetSocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createMulticastSocket(capturedAddress.capture());
            }

            InetSocketAddress address = capturedAddress.getValue();
            assertSame(bindAddress, address.getAddress());
            assertEquals(bindPort, address.getPort());
        }
        reset(socket, this.manager);

        if (bindingName != null) {
            when(this.manager.createMulticastSocket(eq(bindingName))).thenReturn(socket);
        } else {
            when(this.manager.createMulticastSocket()).thenReturn(socket);
        }

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, null)) {
            assertSame(socket, result);
        }
        reset(socket, this.manager);

        SocketAddress socketAddress = new InetSocketAddress(bindAddress, bindPort);
        if (bindingName != null) {
            when(this.manager.createMulticastSocket(eq(bindingName), any())).thenReturn(socket);
        } else {
            when(this.manager.createMulticastSocket(ArgumentMatchers.<SocketAddress>any())).thenReturn(socket);
        }

        try (MulticastSocket result = this.subject.createMulticastSocket(serviceName, socketAddress)) {
            assertSame(socket, result);

            ArgumentCaptor<SocketAddress> capturedAddress = ArgumentCaptor.forClass(SocketAddress.class);
            if (bindingName != null) {
                verify(this.manager).createMulticastSocket(eq(bindingName), capturedAddress.capture());
            } else {
                verify(this.manager).createMulticastSocket(capturedAddress.capture());
            }

            assertSame(socketAddress, capturedAddress.getValue());
        }
    }

    @Test
    public void createSocketChannel() throws IOException {
        this.createSocketChannel("known-service", "binding");
        this.createSocketChannel("unknown-service", null);
    }

    private void createSocketChannel(String serviceName, String bindingName) throws IOException {
        NamedManagedBindingRegistry namedRegistry = mock(NamedManagedBindingRegistry.class);
        UnnamedBindingRegistry unnamedRegistry = mock(UnnamedBindingRegistry.class);

        when(this.manager.getNamedRegistry()).thenReturn(namedRegistry);
        when(this.manager.getUnnamedRegistry()).thenReturn(unnamedRegistry);

        Closeable namedRegistration = mock(Closeable.class);
        Closeable unnamedRegistration = mock(Closeable.class);

        // Validate registration after connect
        try (SocketChannel channel = SocketChannel.open()) {
            when(this.provider.openSocketChannel()).thenReturn(channel);
            when(namedRegistry.registerChannel(eq(bindingName), same(channel))).thenReturn(namedRegistration);
            when(unnamedRegistry.registerChannel(same(channel))).thenReturn(unnamedRegistration);

            SocketChannel result = this.subject.createSocketChannel(serviceName);

            assertSame(channel, result);

            // If registration was successful, close of channel should trigger registration close
            this.subject.close(result);

            verify((bindingName != null) ? namedRegistration : unnamedRegistration).close();
            verify((bindingName == null) ? namedRegistration : unnamedRegistration, never()).close();
        }
    }

    @Test
    public void createServerSocketChannel() throws IOException {
        this.createServerSocketChannel("known-service", "binding");
        this.createServerSocketChannel("unknown-service", null);
    }

    private void createServerSocketChannel(String serviceName, String bindingName) throws IOException {
        NamedManagedBindingRegistry namedRegistry = mock(NamedManagedBindingRegistry.class);
        UnnamedBindingRegistry unnamedRegistry = mock(UnnamedBindingRegistry.class);

        when(this.manager.getNamedRegistry()).thenReturn(namedRegistry);
        when(this.manager.getUnnamedRegistry()).thenReturn(unnamedRegistry);

        Closeable namedRegistration = mock(Closeable.class);
        Closeable unnamedRegistration = mock(Closeable.class);

        // Validate registration after bind
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            when(this.provider.openServerSocketChannel()).thenReturn(channel);
            when(namedRegistry.registerChannel(eq(bindingName), same(channel))).thenReturn(namedRegistration);
            when(unnamedRegistry.registerChannel(same(channel))).thenReturn(unnamedRegistration);

            ServerSocketChannel result = this.subject.createServerSocketChannel(serviceName);

            assertSame(channel, result);

            // If registration was successful, close of channel should trigger registration close
            this.subject.close(result);

            verify((bindingName != null) ? namedRegistration : unnamedRegistration).close();
            verify((bindingName == null) ? namedRegistration : unnamedRegistration, never()).close();
        }
    }

    @Test
    public void closeSocket() throws IOException {

        Socket socket = mock(Socket.class);

        this.subject.close(socket);

        verify(socket).close();
    }

    @Test
    public void closeServerSocket() throws IOException {

        ServerSocket socket = mock(ServerSocket.class);

        this.subject.close(socket);

        verify(socket).close();
    }

    @Test
    public void closeDatagramSocket() {

        DatagramSocket socket = mock(DatagramSocket.class);

        this.subject.close(socket);

        verify(socket).close();
    }

    @Test
    public void closeMulticastSocket() {

        MulticastSocket socket = mock(MulticastSocket.class);

        this.subject.close(socket);

        verify(socket).close();
    }
}
