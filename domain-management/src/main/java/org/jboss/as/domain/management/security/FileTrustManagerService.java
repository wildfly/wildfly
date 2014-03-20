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
package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * An extension to {@link AbstractTrustManagerService} so that a TrustManager[] can be provided based on a JKS file based key
 * store.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class FileTrustManagerService extends AbstractTrustManagerService {

    private final InjectedValue<String> relativeTo = new InjectedValue<String>();

    private volatile String provider;
    private volatile String path;
    private volatile char[] keystorePassword;

    private volatile TrustManagerFactory trustManagerFactory;
    private volatile FileKeystore keyStore;

    FileTrustManagerService(final String provider, final String path, final char[] keystorePassword) {
        this.provider = provider;
        this.path = path;
        this.keystorePassword = keystorePassword;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public char[] getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(char[] keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    /*
     * Service Lifecycle Methods
     */

    @Override
    public void start(StartContext context) throws StartException {
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw MESSAGES.unableToStart(e);
        }
        // Do our initialisation first as the parent implementation will
        // expect that to be complete.
        String relativeTo = this.relativeTo.getOptionalValue();
        String file = relativeTo == null ? path : relativeTo + "/" + path;
        keyStore = FileKeystore.newTrustStore(provider, file, keystorePassword);
        keyStore.load();

        super.start(context);
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
        keyStore = null;
    }

    @Override
    protected TrustManager[] createTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        KeyStore trustStore = loadTrustStore();
        trustManagerFactory.init(trustStore);

        TrustManager[] tmpTrustManagers = trustManagerFactory.getTrustManagers();
        TrustManager[] trustManagers = new TrustManager[tmpTrustManagers.length];
        for (int i = 0; i < tmpTrustManagers.length; i++) {
            trustManagers[i] = new DelegatingTrustManager((X509TrustManager) tmpTrustManagers[i], keyStore);
        }

        return trustManagers;
    }

    @Override
    protected KeyStore loadTrustStore() {
        return keyStore.getKeyStore();
    }

    private class DelegatingTrustManager implements X509TrustManager {

        private X509TrustManager delegate;
        private final FileKeystore theTrustStore;

        private DelegatingTrustManager(X509TrustManager trustManager, FileKeystore theTrustStore) {
            this.delegate = trustManager;
            this.theTrustStore = theTrustStore;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            getDelegate().checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            getDelegate().checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return getDelegate().getAcceptedIssuers();
        }

        /*
         * Internal Methods
         */
        private synchronized X509TrustManager getDelegate() {
            if (theTrustStore.isModified()) {
                try {
                    theTrustStore.load();
                } catch (StartException e1) {
                    throw DomainManagementMessages.MESSAGES.unableToLoadKeyTrustFile(e1.getCause());
                }
                try {
                    trustManagerFactory.init(theTrustStore.getKeyStore());
                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                    for (TrustManager current : trustManagers) {
                        if (current instanceof X509TrustManager) {
                            delegate = (X509TrustManager) current;
                            break;
                        }
                    }
                } catch (GeneralSecurityException e) {
                    throw DomainManagementMessages.MESSAGES.unableToOperateOnTrustStore(e);

                }
            }
            if (delegate == null) {
                throw DomainManagementMessages.MESSAGES.unableToCreateDelegateTrustManager();
            }

            return delegate;
        }

    }

    public InjectedValue<String> getRelativeToInjector() {
        return relativeTo;
    }

}
