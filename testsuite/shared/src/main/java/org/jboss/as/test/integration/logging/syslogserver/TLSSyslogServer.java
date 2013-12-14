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
package org.jboss.as.test.integration.logging.syslogserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.productivity.java.syslog4j.SyslogRuntimeException;
import org.productivity.java.syslog4j.server.impl.net.tcp.ssl.SSLTCPNetSyslogServerConfigIF;

/**
 * TCP syslog server implementation for syslog4j.
 *
 * @author Josef Cacek
 */
public class TLSSyslogServer extends TCPSyslogServer {

    private static final Logger LOGGER = Logger.getLogger(TLSSyslogServer.class);

    private SSLContext sslContext;

    /**
     * Creates custom sslContext from keystore and truststore configured in
     *
     * @see org.productivity.java.syslog4j.server.impl.net.tcp.TCPNetSyslogServer#initialize()
     */
    @Override
    public void initialize() throws SyslogRuntimeException {
        super.initialize();

        // remove BouncyCastle provider if installed
        Security.removeProvider("BC");

        final SSLTCPNetSyslogServerConfigIF config = (SSLTCPNetSyslogServerConfigIF) this.tcpNetSyslogServerConfig;

        try {
            final char[] keystorePwd = config.getKeyStorePassword().toCharArray();
            final KeyStore keystore = loadKeyStore(config.getKeyStore(), keystorePwd);
            final char[] truststorePassword = config.getTrustStorePassword().toCharArray();
            final KeyStore truststore = loadKeyStore(config.getTrustStore(), truststorePassword);

            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePwd);

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory
                    .getDefaultAlgorithm());
            trustManagerFactory.init(truststore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        } catch (Exception e) {
            LOGGER.error("Exception occured during SSLContext for TLS syslog server initialization", e);
            throw new SyslogRuntimeException(e);
        }
    }

    /**
     * Returns {@link ServerSocketFactory} from custom {@link SSLContext} instance created in {@link #initialize()} method.
     *
     * @see org.productivity.java.syslog4j.server.impl.net.tcp.TCPNetSyslogServer#getServerSocketFactory()
     */
    @Override
    protected ServerSocketFactory getServerSocketFactory() throws IOException {
        return sslContext.getServerSocketFactory();
    }

    /**
     * Loads a JKS keystore with given path and password.
     *
     * @param keystoreFile path to keystore file
     * @param keystorePwd keystore password
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    private static KeyStore loadKeyStore(final String keystoreFile, final char[] keystorePwd) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        final KeyStore keystore = KeyStore.getInstance("JKS");
        InputStream is = null;
        try {
            is = new FileInputStream(keystoreFile);
            keystore.load(is, keystorePwd);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return keystore;
    }

}
