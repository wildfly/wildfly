/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.SecurityConstants;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

import com.sun.corba.se.spi.orb.ORB;

/**
 * <p>
 *  This class is responsible for creating Sockets used by IIOP subsystem.
 * <p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class LegacySSLSocketFactory extends SocketFactoryBase {

    private static String securityDomain = null;

    public static void setSecurityDomain(final String securityDomain) {
        LegacySSLSocketFactory.securityDomain = securityDomain;
    }

    private ORB orb;

    private SSLContext sslContext = null;

    private JSSESecurityDomain jsseSecurityDomain = null;

    private boolean request_mutual_auth = false;

    private boolean require_mutual_auth = false;

    @Override
    public void setORB(ORB orb) {
        super.setORB(orb);
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
        if (type.equals(Constants.SSL_SOCKET_TYPE)) {
            return createSSLServerSocket(inetSocketAddress.getPort(), 1000,
                    InetAddress.getByName(inetSocketAddress.getHostName()));
        } else {
            return super.createServerSocket(type, inetSocketAddress);
        }
    }

    @Override
    public Socket createSocket(String type, InetSocketAddress inetSocketAddress) throws IOException {
        if (type.contains(Constants.SSL_SOCKET_TYPE)){
            return createSSLSocket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        } else {
            return super.createSocket(type, inetSocketAddress);
        }
    }

    public Socket createSSLSocket(String host, int port) throws IOException {
        this.initSSLContext();
        InetAddress address = InetAddress.getByName(host);

        javax.net.ssl.SSLSocketFactory socketFactory = this.sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(address, port);
        if (this.jsseSecurityDomain.getProtocols() != null){
            socket.setEnabledProtocols(this.jsseSecurityDomain.getProtocols());
        }
        if (this.jsseSecurityDomain.getCipherSuites() != null) {
            socket.setEnabledCipherSuites(this.jsseSecurityDomain.getCipherSuites());
        }
        socket.setNeedClientAuth(this.jsseSecurityDomain.isClientAuth());
        return socket;
    }

    public ServerSocket createSSLServerSocket(int port, int backlog, InetAddress inetAddress) throws IOException {
        this.initSSLContext();
        SSLServerSocketFactory serverSocketFactory = this.sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port, backlog, inetAddress);
        if (this.jsseSecurityDomain.getProtocols() != null){
            serverSocket.setEnabledProtocols(this.jsseSecurityDomain.getProtocols());
        }
        if (this.jsseSecurityDomain.getCipherSuites() != null){
            serverSocket.setEnabledCipherSuites(this.jsseSecurityDomain.getCipherSuites());
        }
        if (this.jsseSecurityDomain.isClientAuth() || this.require_mutual_auth){
            serverSocket.setNeedClientAuth(true);
        } else {
            serverSocket.setWantClientAuth(this.request_mutual_auth);
        }
        return serverSocket;
    }

    private void initSSLContext() throws IOException {
        if (this.sslContext != null) {
            return;
        }
        this.sslContext = Util.forDomain(this.jsseSecurityDomain);
    }
}
