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

package org.jboss.as.jdkorb.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.sun.corba.se.impl.transport.DefaultSocketFactoryImpl;
import com.sun.corba.se.spi.orb.ORB;
import com.sun.corba.se.spi.transport.SocketInfo;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.JSSESecurityDomain;

public class JDKSocketFactory extends DefaultSocketFactoryImpl implements Service<JDKSocketFactory> {

    private SSLContext sslContext;

    private final InjectedValue<JSSESecurityDomain> jsseSecurityDomain = new InjectedValue<JSSESecurityDomain>();

    private final boolean request_mutual_auth;

    private final boolean require_mutual_auth;

    private ORB orb;

    public JDKSocketFactory(/*final boolean request_mutual_auth, final boolean require_mutual_auth*/) {
        this.request_mutual_auth = false;
        this.require_mutual_auth = false;
    }

    public void setORB(ORB orb) {
        super.setORB(orb);
        this.orb = orb;
    }

    private ServerSocket createServerSocket(int port, int backlog, InetAddress inetAddress) throws IOException {
        final JSSESecurityDomain jsseSecurityDomain = this.jsseSecurityDomain.getValue();
        SSLServerSocketFactory serverSocketFactory = this.sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port, backlog, inetAddress);
        if (jsseSecurityDomain.getProtocols() != null)
            serverSocket.setEnabledProtocols(jsseSecurityDomain.getProtocols());
        if (jsseSecurityDomain.getCipherSuites() != null)
            serverSocket.setEnabledCipherSuites(jsseSecurityDomain.getCipherSuites());

        if (jsseSecurityDomain.isClientAuth() || this.require_mutual_auth)
            serverSocket.setNeedClientAuth(true);
        else
            serverSocket.setWantClientAuth(this.request_mutual_auth);

        return serverSocket;
    }

    @Override
    public ServerSocket createServerSocket(final String type, final InetSocketAddress inetSocketAddress) throws IOException {
        if(type.equals(SocketInfo.IIOP_CLEAR_TEXT)) {
            return super.createServerSocket(type, inetSocketAddress);
        } else {
            return createServerSocket(inetSocketAddress.getPort(), 50, inetSocketAddress.getAddress());
        }
    }

    @Override
    public void start(final StartContext context) throws StartException {
        try {
            this.sslContext = Util.forDomain(this.jsseSecurityDomain.getValue());
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(final StopContext context) {
        this.sslContext = null;
    }

    @Override
    public JDKSocketFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
