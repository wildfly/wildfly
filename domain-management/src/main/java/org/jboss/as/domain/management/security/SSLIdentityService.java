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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.KEYSTORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.jboss.dmr.ModelNode;
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
public class SSLIdentityService implements Service<SSLIdentityService> {

    public static final String SERVICE_SUFFIX = "ssl";

    private final ModelNode ssl;
    private final InjectedValue<String> relativeTo = new InjectedValue<String>();

    private SSLContext sslContext;

    public SSLIdentityService(ModelNode ssl) {
        this.ssl = ssl;
    }

    public void start(StartContext context) throws StartException {
        try {
            KeyManager[] keyManagers = null;

            String protocol = "TLS";
            if (ssl.has(PROTOCOL)) {
                protocol = ssl.get(PROTOCOL).asString();
            }

            if (ssl.has(KEYSTORE)) {
                ModelNode keystoreNode = ssl.get(KEYSTORE);

                String relativeTo = this.relativeTo.getOptionalValue();
                String path = keystoreNode.require(PATH).asString();
                String file = relativeTo == null ? path : relativeTo + "/" + path;
                char[] password = keystoreNode.require(PASSWORD).asString().toCharArray();

                // TODO - Support different KeyStore types?
                KeyStore keystore = KeyStore.getInstance("JKS");
                // TODO - Safer way to read from filesystem?
                FileInputStream fis = new FileInputStream(file);
                keystore.load(fis, password);

                // TODO - Support configuration of KeyManagerFactory?
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(keystore, password);
                keyManagers = keyManagerFactory.getKeyManagers();
            }

            SSLContext sslContext = SSLContext.getInstance(protocol);
            sslContext.init(keyManagers, null, null);

            this.sslContext = sslContext;
        } catch (NoSuchAlgorithmException nsae) {
            throw new StartException("Unable to start service", nsae);
        } catch (KeyManagementException kme) {
            throw new StartException("Unable to start service", kme);
        } catch (KeyStoreException kse) {
            throw new StartException("Unable to start service", kse);
        } catch (FileNotFoundException fnfe) {
            throw new StartException("Unable to start service", fnfe);
        } catch (CertificateException e) {
            throw new StartException("Unable to start service", e);
        } catch (IOException e) {
            throw new StartException("Unable to start service", e);
        } catch (UnrecoverableKeyException e) {
            throw new StartException("Unable to start service", e);
        }
    }

    public void stop(StopContext context) {
    }

    public SSLIdentityService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<String> getRelativeToInjector() {
        return relativeTo;
    }

    SSLContext getSSLContext() {
        return sslContext;
    }

}
