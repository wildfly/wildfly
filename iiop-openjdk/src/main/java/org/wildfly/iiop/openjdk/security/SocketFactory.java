/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.SecurityConstants;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

import com.sun.corba.se.impl.orbutil.ORBConstants;
import com.sun.corba.se.pept.transport.Acceptor;
import com.sun.corba.se.spi.orb.ORB;
import com.sun.corba.se.spi.transport.ORBSocketFactory;

/**
 * <p>
 *  This class is responsible for creating Sockets used by IIOP subsystem.
 * <p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class SocketFactory implements ORBSocketFactory {

    private static String securityDomain = null;

    public static void setSecurityDomain(final String securityDomain) {
        SocketFactory.securityDomain = securityDomain;
    }

    private ORB orb;

    private SSLContext sslContext = null;

    private JSSESecurityDomain jsseSecurityDomain = null;

    private boolean request_mutual_auth = false;

    private boolean require_mutual_auth = false;

    @Override
    public void setORB(ORB orb) {
        this.orb = orb;

        try {
            InitialContext context = new InitialContext();
            jsseSecurityDomain = (JSSESecurityDomain) context.lookup(SecurityConstants.JAAS_CONTEXT_ROOT + securityDomain
                    + "/jsse");
            IIOPLogger.ROOT_LOGGER.debugf("Obtained JSSE security domain with name %s", securityDomain);
        } catch (NamingException ne) {
            IIOPLogger.ROOT_LOGGER.failedToObtainJSSEDomain(securityDomain);
        }
        if (jsseSecurityDomain == null)
            throw new RuntimeException(IIOPLogger.ROOT_LOGGER.failedToLookupJSSEDomain());
    }

    @Override
    public ServerSocket createServerSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        ServerSocketChannel serverSocketChannel = null;
        ServerSocket serverSocket = null;

        if (type.equals(Constants.SSL_SOCKET_TYPE)) {
            serverSocket = createSSLServerSocket(inetSocketAddress.getPort(), 1000,
                    InetAddress.getByName(inetSocketAddress.getHostName()));
        } else if (orb.getORBData().acceptorSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocket = serverSocketChannel.socket();
        } else {
            serverSocket = new ServerSocket();
        }
        if (!type.equals(Constants.SSL_SOCKET_TYPE)) {
            serverSocket.bind(inetSocketAddress);
        }
        return serverSocket;
    }

    public Socket createSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        SocketChannel socketChannel = null;
        Socket socket = null;

        if (type.contains(Constants.SSL_SOCKET_TYPE)) {
            socket = createSSLSocket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        } else if (orb.getORBData().connectionSocketType().equals(ORBConstants.SOCKETCHANNEL)) {
            socketChannel = SocketChannel.open(inetSocketAddress);
            socket = socketChannel.socket();
        } else {
            socket = new Socket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        }

        // Disable Nagle's algorithm (i.e., always send immediately).
        socket.setTcpNoDelay(true);
        return socket;
    }

    public void setAcceptedSocketOptions(Acceptor acceptor, ServerSocket serverSocket, Socket socket) throws SocketException {
        // Disable Nagle's algorithm (i.e., always send immediately).
        socket.setTcpNoDelay(true);
    }

    public Socket createSSLSocket(String host, int port) throws IOException {
        this.initSSLContext();
        InetAddress address = InetAddress.getByName(host);

        SSLSocketFactory socketFactory = this.sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(address, port);
        if (this.jsseSecurityDomain.getProtocols() != null)
            socket.setEnabledProtocols(this.jsseSecurityDomain.getProtocols());
        if (this.jsseSecurityDomain.getCipherSuites() != null)
            socket.setEnabledCipherSuites(this.jsseSecurityDomain.getCipherSuites());
        socket.setNeedClientAuth(this.jsseSecurityDomain.isClientAuth());
        return socket;
    }

    public ServerSocket createSSLServerSocket(int port, int backlog, InetAddress inetAddress) throws IOException {
        this.initSSLContext();
        SSLServerSocketFactory serverSocketFactory = this.sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port, backlog, inetAddress);
        if (this.jsseSecurityDomain.getProtocols() != null)
            serverSocket.setEnabledProtocols(this.jsseSecurityDomain.getProtocols());
        if (this.jsseSecurityDomain.getCipherSuites() != null)
            serverSocket.setEnabledCipherSuites(this.jsseSecurityDomain.getCipherSuites());

        if (this.jsseSecurityDomain.isClientAuth() || this.require_mutual_auth)
            serverSocket.setNeedClientAuth(true);
        else
            serverSocket.setWantClientAuth(this.request_mutual_auth);

        return serverSocket;
    }

    private void initSSLContext() throws IOException {
        if (this.sslContext != null)
            return;
        this.sslContext = Util.forDomain(this.jsseSecurityDomain);
    }
}
