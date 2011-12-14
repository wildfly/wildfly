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

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service to handle loading Keystores from file so that the Keystore can be injected ready for SSLContext creation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class FileKeystoreService implements Service<KeyStore> {

    public static final String KEYSTORE_SUFFIX = "keystore";
    public static final String TRUSTSTORE_SUFFIX = "truststore";


    private KeyStore theKeyStore;
    private final String path;
    private final char[] password;

    private final InjectedValue<String> relativeTo = new InjectedValue<String>();

    public FileKeystoreService(final String path, final char[] password) {
        this.path = path;
        this.password = password;
    }

    public void start(StartContext ctx) throws StartException {
        String relativeTo = this.relativeTo.getOptionalValue();
        String file = relativeTo == null ? path : relativeTo + "/" + path;

        FileInputStream fis = null;
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            fis = new FileInputStream(file);
            keystore.load(fis, password);

            this.theKeyStore = keystore;
        } catch (KeyStoreException e) {
            throw MESSAGES.unableToStart(e);
        } catch (NoSuchAlgorithmException e) {
            throw MESSAGES.unableToStart(e);
        } catch (CertificateException e) {
            throw MESSAGES.unableToStart(e);
        } catch (IOException e) {
            throw MESSAGES.unableToStart(e);
        } finally {
            safeClose(fis);
        }
    }

    public void stop(StopContext ctx) {
        theKeyStore = null;
    }

    public KeyStore getValue() throws IllegalStateException, IllegalArgumentException {
        return theKeyStore;
    }

    public InjectedValue<String> getRelativeToInjector() {
        return relativeTo;
    }

    private void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

}
