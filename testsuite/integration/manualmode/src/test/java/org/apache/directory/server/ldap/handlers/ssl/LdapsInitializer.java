/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.apache.directory.server.ldap.handlers.ssl;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.filter.ssl.SslFilter;
import org.jboss.as.test.manualmode.security.TrustAndStoreTrustManager;

/**
 * Re-implementation of LdapsInitializer from ApacheDS project, for testing purposes. This version ask for client authentication
 * during SSL handshake and as a TrustManager uses {@link TrustAndStoreTrustManager} instance.
 *
 * @author Josef Cacek
 */
public class LdapsInitializer {

    // Public methods --------------------------------------------------------

    /**
     * Initializes LDAPs with optional client certificate authentication used for underlying SSL.
     *
     * @param keystore
     * @param keystorePwd
     * @return
     * @throws LdapException
     */
    public static IoFilterChainBuilder init(KeyStore keystore, String keystorePwd) throws LdapException {
        final SslFilter sslFilter = new SslFilter(createSSLContext(keystore, keystorePwd));
        // ask for client authentication
        sslFilter.setWantClientAuth(true);

        final DefaultIoFilterChainBuilder ioFilterChainBuilder = new DefaultIoFilterChainBuilder();
        ioFilterChainBuilder.addLast("sslFilter", sslFilter);

        return ioFilterChainBuilder;
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates {@link SSLContext} initialized with KeyManager from given keystore, as a TrustManager is used
     * {@link TrustAndStoreTrustManager} instance.
     *
     * @param keystore
     * @param keystorePwd
     * @return SSLContext
     * @throws LdapException if creation of {@link SSLContext} fails
     */
    private static SSLContext createSSLContext(KeyStore keystore, String keystorePwd) throws LdapException {
        try {
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePwd.toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { new TrustAndStoreTrustManager() }, null);
            return sslContext;
        } catch (Exception e) {
            throw new LdapException("Creating SSL context failed.", e);
        }
    }
}
