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
import java.net.ServerSocket;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.jacorb.config.Configurable;
import org.jacorb.config.Configuration;
import org.jacorb.config.ConfigurationException;
import org.jacorb.orb.ORB;
import org.jacorb.orb.factory.ServerSocketFactory;
import org.jboss.as.jacorb.JacORBSubsystemConstants;
import org.jboss.as.jacorb.PropertiesMap;
import org.jboss.logging.Logger;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.SecurityConstants;

/**
 * <p>
 * This class implements a JacORB {@code ServerSocketFactory} that uses a {@code JSSESecurityDomain} to build SSL server
 * sockets. It is installed if SSL support is enabled in the subsystem configuration. For example:
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
public class DomainServerSocketFactory implements ServerSocketFactory, Configurable {

    private static final Logger log = Logger.getLogger("org.jboss.as.security");

    private SSLContext sslContext;

    private JSSESecurityDomain jsseSecurityDomain;

    private boolean request_mutual_auth = false;

    private boolean require_mutual_auth = false;

    /**
     * <p>
     * Creates an instance of {@code DomainServerSocketFactory} with the specified ORB.
     * </p>
     *
     * @param orb a reference to the running {@code org.jacorb.orb.ORB} instance.
     */
    public DomainServerSocketFactory(ORB orb) {
        log.tracef("Creating server socket factory: %s", this.getClass().getName());
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return this.createServerSocket(port, 50);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        return this.createServerSocket(port, 50, null);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress inetAddress) throws IOException {
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

    @Override
    public void configure(Configuration configuration) throws ConfigurationException {
        // get the configured security domain name.
        String securityDomain = configuration.getAttribute(JacORBSubsystemConstants.SECURITY_SECURITY_DOMAIN);
        if (securityDomain == null)
            throw new ConfigurationException("Error configuring domain server socket factory: security domain is null");

        // use the security domain name to obtain the JSSE security domain.
        try {
            InitialContext context = new InitialContext();
            this.jsseSecurityDomain = (JSSESecurityDomain) context.lookup(SecurityConstants.JAAS_CONTEXT_ROOT +
                    securityDomain + "/jsse");
            log.debugf("Obtained JSSE security domain with name %s", securityDomain);
        } catch (NamingException ne) {
            log.errorf("Failed to obtain JSSE security domain with name %s", securityDomain);
        }
        if (this.jsseSecurityDomain == null)
            throw new ConfigurationException("Error configuring domain server socket factory: failed to lookup JSSE security domain");

        // check if mutual auth is requested or required.
        String optionName = PropertiesMap.JACORB_PROPS_MAP.get(JacORBSubsystemConstants.SECURITY_SERVER_SUPPORTS);
        short serverSupportedOptions = Short.parseShort(configuration.getAttribute(optionName), 16); // value is hexadecimal.
        optionName = PropertiesMap.JACORB_PROPS_MAP.get(JacORBSubsystemConstants.SECURITY_SERVER_REQUIRES);
        short serverRequiredOptions = Short.parseShort(configuration.getAttribute(optionName), 16);

        if ((serverSupportedOptions & 0x40) != 0) {
            // would prefer to establish trust in client. If client can support mutual auth, it will, otherwise it won't.
            this.request_mutual_auth = true;
        }
        if ((serverRequiredOptions & 0x40) != 0) {
            // required: force client to authenticate.
            this.require_mutual_auth = true;
        }
    }

    /**
     * <p>
     * Initializes the {@code SSLContext} if necessary.
     * </p>
     *     * @throws IOException if an error occurs while initializing the {@code SSLContext}.
     */
    private void initSSLContext() throws IOException {
        if (this.sslContext != null)
            return;
        this.sslContext = Util.forDomain(this.jsseSecurityDomain);
    }
}
