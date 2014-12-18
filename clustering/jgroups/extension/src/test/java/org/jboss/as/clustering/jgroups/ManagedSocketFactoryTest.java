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

import org.jboss.as.network.SocketBindingManager;
import org.jgroups.util.SocketFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public class ManagedSocketFactoryTest {

    private SocketFactory factory = mock(SocketFactory.class);
    private SocketBindingManager manager = mock(SocketBindingManager.class);
    private SocketBindingManager.UnnamedBindingRegistry registry = mock(SocketBindingManager.UnnamedBindingRegistry.class);

    private ManagedSocketFactory subject = new ManagedSocketFactory(this.factory, this.manager);

    @Before
    public void setUp() {
        when(this.manager.getUnnamedRegistry()).thenReturn(registry);
    }

    @Test
    public void createSocket() throws IOException {

        Socket socket1 = new Socket();
        Socket socket2 = new Socket();
        Socket socket3 = new Socket();
        Socket socket4 = new Socket();
        Socket socket5 = new Socket();
        InetAddress localhost = InetAddress.getLocalHost();

        when(this.factory.createSocket("test")).thenReturn(socket1);
        when(this.factory.createSocket("test", localhost, 1)).thenReturn(socket2);
        when(this.factory.createSocket("test", "host", 1)).thenReturn(socket3);
        when(this.factory.createSocket("test", localhost, 1, localhost, 2)).thenReturn(socket4);
        when(this.factory.createSocket("test", "host", 1, localhost, 2)).thenReturn(socket5);

        Socket result1 = this.subject.createSocket("test");
        Socket result2 = this.subject.createSocket("test", localhost, 1);
        Socket result3 = this.subject.createSocket("test", "host", 1);
        Socket result4 = this.subject.createSocket("test", localhost, 1, localhost, 2);
        Socket result5 = this.subject.createSocket("test", "host", 1, localhost, 2);

        verify(this.manager.getUnnamedRegistry()).registerSocket(socket1);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket2);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket3);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket4);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket5);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
        assertSame(socket4, result4);
        assertSame(socket5, result5);
    }

    @Test
    public void createServerSocket() throws IOException {

        ServerSocket socket1 = new ServerSocket();
        ServerSocket socket2 = new ServerSocket();
        ServerSocket socket3 = new ServerSocket();
        ServerSocket socket4 = new ServerSocket();
        InetAddress localhost = InetAddress.getLocalHost();

        when(this.factory.createServerSocket("test")).thenReturn(socket1);
        when(this.factory.createServerSocket("test", 1)).thenReturn(socket2);
        when(this.factory.createServerSocket("test", 1, 0)).thenReturn(socket3);
        when(this.factory.createServerSocket("test", 1, 0, localhost)).thenReturn(socket4);

        ServerSocket result1 = this.subject.createServerSocket("test");
        ServerSocket result2 = this.subject.createServerSocket("test", 1);
        ServerSocket result3 = this.subject.createServerSocket("test", 1, 0);
        ServerSocket result4 = this.subject.createServerSocket("test", 1, 0, localhost);

        verify(this.manager.getUnnamedRegistry()).registerSocket(socket1);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket2);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket3);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket4);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
        assertSame(socket4, result4);
    }

    @Test
    public void createDatagram() throws IOException {

        DatagramSocket socket1 = new DatagramSocket();
        DatagramSocket socket2 = new DatagramSocket();
        DatagramSocket socket3 = new DatagramSocket();
        DatagramSocket socket4 = new DatagramSocket();
        InetAddress localhost = InetAddress.getLocalHost();
        SocketAddress socketAddress = new InetSocketAddress(localhost, 1);

        when(this.factory.createDatagramSocket("test")).thenReturn(socket1);
        when(this.factory.createDatagramSocket("test", 1)).thenReturn(socket2);
        when(this.factory.createDatagramSocket("test", socketAddress)).thenReturn(socket3);
        when(this.factory.createDatagramSocket("test", 1, localhost)).thenReturn(socket4);

        DatagramSocket result1 = this.subject.createDatagramSocket("test");
        DatagramSocket result2 = this.subject.createDatagramSocket("test", 1);
        DatagramSocket result3 = this.subject.createDatagramSocket("test", socketAddress);
        DatagramSocket result4 = this.subject.createDatagramSocket("test", 1, localhost);

        verify(this.manager.getUnnamedRegistry()).registerSocket(socket1);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket2);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket3);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket4);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
        assertSame(socket4, result4);
    }

    @Test
    public void createMulticastSocket() throws IOException {

        MulticastSocket socket1 = new MulticastSocket();
        MulticastSocket socket2 = new MulticastSocket();
        MulticastSocket socket3 = new MulticastSocket();
        SocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 1);

        when(this.factory.createMulticastSocket("test")).thenReturn(socket1);
        when(this.factory.createMulticastSocket("test", 1)).thenReturn(socket2);
        when(this.factory.createMulticastSocket("test", address)).thenReturn(socket3);

        MulticastSocket result1 = this.subject.createMulticastSocket("test");
        MulticastSocket result2 = this.subject.createMulticastSocket("test", 1);
        MulticastSocket result3 = this.subject.createMulticastSocket("test", address);

        verify(this.manager.getUnnamedRegistry()).registerSocket(socket1);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket2);
        verify(this.manager.getUnnamedRegistry()).registerSocket(socket3);

        assertSame(socket1, result1);
        assertSame(socket2, result2);
        assertSame(socket3, result3);
    }

    @Test
    public void closeSocket() throws IOException {

        Socket socket = new Socket();

        this.subject.close(socket);

        verify(this.factory).close(socket);
        verify(this.manager.getUnnamedRegistry()).unregisterSocket(socket);
    }

    @Test
    public void closeServerSocket() throws IOException {

        ServerSocket socket = new ServerSocket();

        this.subject.close(socket);

        verify(this.factory).close(socket);
        verify(this.manager.getUnnamedRegistry()).unregisterSocket(socket);
    }

    @Test
    public void closeDatagramSocket() throws IOException {

        DatagramSocket socket = new DatagramSocket();

        this.subject.close(socket);

        verify(this.factory).close(socket);
        verify(this.manager.getUnnamedRegistry()).unregisterSocket(socket);
    }

    @Test
    public void closeMulticastSocket() throws IOException {

        MulticastSocket socket = new MulticastSocket();

        this.subject.close(socket);

        verify(this.factory).close(socket);
        verify(this.manager.getUnnamedRegistry()).unregisterSocket(socket);
    }
}
