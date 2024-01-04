/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.security;

import static org.wildfly.security.manager.WildFlySecurityManager.getPropertyPrivileged;
import com.sun.corba.se.impl.orbutil.ORBConstants;
import com.sun.corba.se.pept.transport.Acceptor;
import com.sun.corba.se.spi.orb.ORB;
import com.sun.corba.se.spi.transport.ORBSocketFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public abstract class SocketFactoryBase implements ORBSocketFactory {

    protected ORB orb;
    // Use an instance field for this so it gets recalculated following a reload
    private final boolean enableTcpKeepAlive = !"false".equalsIgnoreCase(getPropertyPrivileged("com.sun.CORBA.transport.enableTcpKeepAlive", "false"));

    @Override
    public void setORB(ORB orb) {
        this.orb = orb;
    }

    public ServerSocket createServerSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        ServerSocketChannel serverSocketChannel = null;
        ServerSocket serverSocket = null;

        if (orb.getORBData().acceptorSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocket = serverSocketChannel.socket();
        } else {
            serverSocket = new ServerSocket();
        }
        serverSocket.bind(inetSocketAddress);
        return serverSocket;
    }

    public Socket createSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        SocketChannel socketChannel = null;
        Socket socket = null;

        if (orb.getORBData().connectionSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
            socketChannel = SocketChannel.open(inetSocketAddress);
            socket = socketChannel.socket();
        } else {
            socket = new Socket(inetSocketAddress.getHostName(),
                    inetSocketAddress.getPort());
        }

        // Disable Nagle's algorithm (i.e., always send immediately).
        socket.setTcpNoDelay(true);

        if (enableTcpKeepAlive) {
            socket.setKeepAlive(true);
        }

        return socket;
    }

    public void setAcceptedSocketOptions(Acceptor acceptor, ServerSocket serverSocket, Socket socket) throws SocketException {
        // Disable Nagle's algorithm (i.e., always send immediately).
        socket.setTcpNoDelay(true);
    }
}