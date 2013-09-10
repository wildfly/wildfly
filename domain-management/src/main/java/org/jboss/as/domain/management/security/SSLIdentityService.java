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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.as.domain.management.SSLIdentity;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service to handle managing the SSL Identity of a security realm.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SSLIdentityService implements Service<SSLIdentity>, SSLIdentity {

    private final String protocol;
    private final char[] keystorePassword;
    private final char[] keyPassword;
    private final InjectedValue<FileKeystore> keystore = new InjectedValue<FileKeystore>();
    private final InjectedValue<FileKeystore> truststore = new InjectedValue<FileKeystore>();

    private volatile SSLContext fullContext;
    private volatile SSLContext trustOnlyContext;

    private TrustManagerFactory trustManagerFactory;

    public SSLIdentityService(String protocol, char[] keystorePassword, char[] keyPassword) {
        this.protocol = protocol;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
    }

    public void start(StartContext context) throws StartException {
        try {
            KeyManager[] keyManagers = null;
            FileKeystore theKeyStore = keystore.getOptionalValue();
            if (theKeyStore != null && theKeyStore.getKeyStore() != null) {
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(theKeyStore.getKeyStore(), keyPassword == null ? keystorePassword : keyPassword);
                keyManagers = keyManagerFactory.getKeyManagers();
            }

            TrustManager[] trustManagers = null;
            FileKeystore theTrustStore = truststore.getOptionalValue();
            if (theTrustStore != null) {
                trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(theTrustStore.getKeyStore());

                TrustManager[] tmpTrustManagers = trustManagerFactory.getTrustManagers();
                trustManagers = new TrustManager[tmpTrustManagers.length];
                for (int i = 0; i < tmpTrustManagers.length; i++) {
                    trustManagers[i] = new DelegatingTrustManager((X509TrustManager) tmpTrustManagers[i], theTrustStore);
                }
            }

            SSLContext sslContext = SSLContext.getInstance(protocol);
            sslContext.init(keyManagers, trustManagers, null);

            this.fullContext = sslContext;

            if (keyManagers != null) {
                // No point re-creating if there was no KeyManager.
                sslContext = SSLContext.getInstance(protocol);
                sslContext.init(null, trustManagers, null);
            }
            trustOnlyContext = sslContext;
        } catch (NoSuchAlgorithmException nsae) {
            throw MESSAGES.unableToStart(nsae);
        } catch (KeyManagementException kme) {
            throw MESSAGES.unableToStart(kme);
        } catch (KeyStoreException kse) {
            throw MESSAGES.unableToStart(kse);
        } catch (UnrecoverableKeyException e) {
            throw MESSAGES.unableToStart(e);
        }
    }

    public void stop(StopContext context) {
    }

    public SSLIdentityService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<FileKeystore> getKeyStoreInjector() {
        return keystore;
    }

    public InjectedValue<FileKeystore> getTrustStoreInjector() {
        return truststore;
    }

    public SSLContext getFullContext() {
        return fullContext;
    }

    public SSLContext getTrustOnlyContext() {
        return trustOnlyContext;
    }

    boolean hasTrustStore() {
        return (truststore.getOptionalValue() != null);
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
}
