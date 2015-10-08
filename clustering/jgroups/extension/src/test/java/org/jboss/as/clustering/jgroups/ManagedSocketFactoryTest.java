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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import org.jboss.as.network.ManagedServerSocketFactory;
import org.jboss.as.network.ManagedSocketFactory;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.util.SocketFactory;
import org.junit.After;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ManagedSocketFactoryTest {

    private SocketBindingManager manager = mock(SocketBindingManager.class);

    private SocketFactory subject = new org.jboss.as.clustering.jgroups.ManagedSocketFactory(this.manager);

    @After
    public void destroy() {
        reset(this.manager);
    }

    @Test
    public void createSocket() throws IOException {

        ManagedSocketFactory factory = mock(ManagedSocketFactory.class);
        Socket socket1 = mock(Socket.class);
        Socket socket2 = mock(Socket.class);
        Socket socket3 = mock(Socket.class);
        Socket socket4 = mock(Socket.class);
        Socket socket5 = mock(Socket.class);
        InetAddress localhost = InetAddress.getLocalHost();

        when(this.manager.getSocketFactory()).thenReturn(factory);
        when(factory.createSocket("test")).thenReturn(socket1);
        when(factory.createSocket("test", localhost, 1)).thenReturn(socket2);
        when(factory.createSocket("test", "host", 1)).thenReturn(socket3);
        when(factory.createSocket("test", localhost, 1, localhost, 2)).thenReturn(socket4);
        when(factory.createSocket("test", "host", 1, localhost, 2)).thenReturn(socket5);

        Socket result1 = this.subject.createSocket("test");
        Socket result2 = this.subject.createSocket("test", localhost, 1);
        Socket result3 = this.subject.createSocket("test", "host", 1);
        Socket result4 = this.subject.createSocket("test", localhost, 1, localhost, 2);
        Socket result5 = this.subject.createSocket("test", "host", 1, localhost, 2);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
        assertSame(socket4, result4);
        assertSame(socket5, result5);
    }

    @Test
    public void createServerSocket() throws IOException {

        ManagedServerSocketFactory factory = mock(ManagedServerSocketFactory.class);
        ServerSocket socket1 = mock(ServerSocket.class);
        ServerSocket socket2 = mock(ServerSocket.class);
        ServerSocket socket3 = mock(ServerSocket.class);
        ServerSocket socket4 = mock(ServerSocket.class);
        InetAddress localhost = InetAddress.getLocalHost();

        when(this.manager.getServerSocketFactory()).thenReturn(factory);
        when(factory.createServerSocket("test")).thenReturn(socket1);
        when(factory.createServerSocket("test", 1)).thenReturn(socket2);
        when(factory.createServerSocket("test", 1, 0)).thenReturn(socket3);
        when(factory.createServerSocket("test", 1, 0, localhost)).thenReturn(socket4);

        ServerSocket result1 = this.subject.createServerSocket("test");
        ServerSocket result2 = this.subject.createServerSocket("test", 1);
        ServerSocket result3 = this.subject.createServerSocket("test", 1, 0);
        ServerSocket result4 = this.subject.createServerSocket("test", 1, 0, localhost);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
        assertSame(socket4, result4);
    }

    @Test
    public void createDatagram() throws IOException {

        DatagramSocket socket1 = mock(DatagramSocket.class);
        DatagramSocket socket2 = mock(DatagramSocket.class);
        DatagramSocket socket3 = mock(DatagramSocket.class);
        DatagramSocket socket4 = mock(DatagramSocket.class);
        InetAddress localhost = InetAddress.getLocalHost();
        SocketAddress socketAddress = new InetSocketAddress(localhost, 2);

        when(this.manager.createDatagramSocket("test", new InetSocketAddress(0))).thenReturn(socket1);
        when(this.manager.createDatagramSocket("test", new InetSocketAddress(1))).thenReturn(socket2);
        when(this.manager.createDatagramSocket("test", socketAddress)).thenReturn(socket3);
        when(this.manager.createDatagramSocket("test", new InetSocketAddress(localhost, 1))).thenReturn(socket4);

        DatagramSocket result1 = this.subject.createDatagramSocket("test");
        DatagramSocket result2 = this.subject.createDatagramSocket("test", 1);
        DatagramSocket result3 = this.subject.createDatagramSocket("test", socketAddress);
        DatagramSocket result4 = this.subject.createDatagramSocket("test", 1, localhost);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
        assertSame(socket4, result4);
    }

    @Test
    public void createMulticastSocket() throws IOException {

        MulticastSocket socket1 = mock(MulticastSocket.class);
        MulticastSocket socket2 = mock(MulticastSocket.class);
        MulticastSocket socket3 = mock(MulticastSocket.class);
        SocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 1);

        when(this.manager.createMulticastSocket("test", new InetSocketAddress(0))).thenReturn(socket1);
        when(this.manager.createMulticastSocket("test", new InetSocketAddress(1))).thenReturn(socket2);
        when(this.manager.createMulticastSocket("test", address)).thenReturn(socket3);

        MulticastSocket result1 = this.subject.createMulticastSocket("test");
        MulticastSocket result2 = this.subject.createMulticastSocket("test", 1);
        MulticastSocket result3 = this.subject.createMulticastSocket("test", address);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
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
