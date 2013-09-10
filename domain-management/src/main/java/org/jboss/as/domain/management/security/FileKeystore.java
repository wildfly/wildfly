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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

/**
 * Store the keystore and last modification date. Able to reload the keystore incase
 * the file has changed
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 *
 */
final class FileKeystore {

    private KeyStore keyStore;
    private final String path;
    private long lastModificationTime;
    private final char[] keystorePassword;
    private final char[] keyPassword;
    private final String alias;

    FileKeystore(final String path, final char[] keystorePassword, final char[] keyPassword,final String alias) {
        this.path = path;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
        this.alias = alias;
        this.lastModificationTime = 0;
    }

    /**
     * @return true if the keystore file is modified since it was loaded the first time
     */
    boolean isModified() {
        long lastModified = new File(path).lastModified();
        if (lastModified > this.lastModificationTime) {
            return true;
        } else if (lastModified == 0) {
            return true;
        }
        return false;
    }

    /**
     * Load the keystore file and cache the keystore. if the keystore file has changed
     * call load() again for updating the keystore
     * @throws StartException
     */
    void load() throws StartException {
        FileInputStream fis = null;
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");

            if (new File(path).exists()) {
                fis = new FileInputStream(path);
                loadedKeystore.load(fis, keystorePassword);
            } else {
                loadedKeystore.load(null);
            }

            if (alias == null) {
                this.setKeyStore(loadedKeystore);
            } else {
                KeyStore newKeystore = KeyStore.getInstance("JKS");
                newKeystore.load(null);

                KeyStore.ProtectionParameter passParam = new KeyStore.PasswordProtection(keyPassword == null ? keystorePassword
                        : keyPassword);
                KeyStore.Entry entry = loadedKeystore.getEntry(this.alias, passParam);
                newKeystore.setEntry(alias, entry, passParam);

                this.setKeyStore(newKeystore);
            }
            this.lastModificationTime = new File(path).lastModified();
        } catch (KeyStoreException e) {
            throw MESSAGES.unableToStart(e);
        } catch (NoSuchAlgorithmException e) {
            throw MESSAGES.unableToStart(e);
        } catch (CertificateException e) {
            throw MESSAGES.unableToStart(e);
        } catch (IOException e) {
            throw MESSAGES.unableToStart(e);
        } catch (UnrecoverableEntryException e) {
            throw MESSAGES.unableToStart(e);
        } finally {
            safeClose(fis);
        }
    }

    /**
     * @return the current cached keystore.
     */
    KeyStore getKeyStore() {
        return keyStore;
    }

    private void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    private void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static final class ServiceUtil {

        private static final String KEYSTORE_SUFFIX = "keystore";
        private static final String TRUSTSTORE_SUFFIX = "truststore";

        private ServiceUtil() {
        }

        public static ServiceName createKeystoreServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(KEYSTORE_SUFFIX);
        }

        public static ServiceName createTrusttoreServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(TRUSTSTORE_SUFFIX);
        }

        public static ServiceBuilder<?> addDependency(ServiceBuilder<?> sb, Injector<FileKeystore> injector,
                ServiceName serviceName, boolean optional) {
            ServiceBuilder.DependencyType type = optional ? ServiceBuilder.DependencyType.OPTIONAL : ServiceBuilder.DependencyType.REQUIRED;
            sb.addDependency(type, serviceName, FileKeystore.class, injector);

            return sb;
        }

    }

}
