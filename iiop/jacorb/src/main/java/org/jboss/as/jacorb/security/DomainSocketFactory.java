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

package org.jboss.as.jacorb.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jacorb.config.Configurable;
import org.jacorb.config.Configuration;
import org.jacorb.config.ConfigurationException;
import org.jacorb.orb.ORB;
import org.jacorb.orb.factory.SocketFactory;
import org.jboss.as.jacorb.JacORBLogger;
import org.jboss.as.jacorb.JacORBMessages;
import org.jboss.as.jacorb.JacORBSubsystemConstants;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.SecurityConstants;
import org.omg.CORBA.TIMEOUT;

/**
 * <p>
 * This class implements a JacORB {@code SocketFactory} that uses a {@code JSSESecurityDomain} to build SSL  sockets.
 * It is installed if SSL support is enabled in the subsystem configuration. For example:
 *
 * <pre>
 *     <subsystem xmlns="urn:jboss:domain:jacorb:1.1">
 *         <orb....>
 *             <initializers security="on"/>
 *         </orb>
 *         <security support-ssl="on" security-domain="ssl-domain"/>
 *     </subsystem>
 * </pre>
 *
 * The security domain name is required and must match a configured domain that contains JSSE configuration:
 *
 * <pre>
 *     <subsystem xmlns="urn:jboss:domain:security:1.1">
 *         <security-domain name="ssl-domain">
 *             <jsse keystore-url="..." keystore-password="..." truststore-url=".." truststore-password="..." />
 *         </security-domain>
 *     </subsystem>
 * </pre>
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class DomainSocketFactory implements SocketFactory, Configurable {

    private SSLContext sslContext;

    private JSSESecurityDomain jsseSecurityDomain;

    /**
     * <p>
     * Creates an instance of {@code DomainSocketFactory} with the specified ORB.
     * </p>
     *
     * @param orb a reference to the running {@code org.jacorb.orb.ORB} instance.
     */
    public DomainSocketFactory(ORB orb) {
        JacORBLogger.ROOT_LOGGER.traceSocketFactoryCreation(this.getClass().getName());
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
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

    @Override
    public Socket createSocket(String host, int port, int timeout) throws IOException, TIMEOUT {
        this.initSSLContext();
        InetAddress address = InetAddress.getByName(host);

        SSLSocketFactory socketFactory = this.sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket();
        socket.connect(new InetSocketAddress(address, port), timeout);
        if (this.jsseSecurityDomain.getProtocols() != null)
            socket.setEnabledProtocols(this.jsseSecurityDomain.getProtocols());
        if (this.jsseSecurityDomain.getCipherSuites() != null)
            socket.setEnabledCipherSuites(this.jsseSecurityDomain.getCipherSuites());
        socket.setNeedClientAuth(this.jsseSecurityDomain.isClientAuth());
        return socket;
    }

    @Override
    public boolean isSSL(Socket socket) {
        return (socket instanceof SSLSocket);
    }

    @Override
    public void configure(Configuration configuration) throws ConfigurationException {
        // get the configured security domain name.
        String securityDomain = configuration.getAttribute(JacORBSubsystemConstants.SECURITY_SECURITY_DOMAIN);
        if (securityDomain == null)
            throw JacORBMessages.MESSAGES.errorConfiguringDomainSF();

        // use the security domain name to obtain the JSSE security domain.
        try {
            InitialContext context = new InitialContext();
            this.jsseSecurityDomain = (JSSESecurityDomain) context.lookup(SecurityConstants.JAAS_CONTEXT_ROOT +
                    securityDomain + "/jsse");
            JacORBLogger.ROOT_LOGGER.debugJSSEDomainRetrieval(securityDomain);
        } catch (NamingException ne) {
            JacORBLogger.ROOT_LOGGER.failedToObtainJSSEDomain(securityDomain);
        }
        if (this.jsseSecurityDomain == null)
            throw JacORBMessages.MESSAGES.failedToLookupJSSEDomain();
    }

    /**
     * <p>
     * Initializes the {@code SSLContext} if necessary.
     * </p>
     *
     * @throws IOException if an error occurs while initializing the {@code SSLContext}.
     */
    private void initSSLContext() throws IOException {
        if (this.sslContext != null)
            return;
        this.sslContext = Util.forDomain(this.jsseSecurityDomain);
    }
}
