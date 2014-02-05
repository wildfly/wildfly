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

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Extension of {@link AbstractTrustManagerService} to load the KeyStore using a specified provider.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ProviderTrustManagerService extends AbstractTrustManagerService {

    private volatile String provider;
    private volatile char[] storePassword;

    private volatile KeyStore theKeyStore;

    ProviderTrustManagerService(final String provider, final char[] storePassword) {
        super();
        this.provider = provider;
        this.storePassword = storePassword;
    }

    @Override
    public void start(StartContext context) throws StartException {

        try {
            KeyStore theKeyStore = KeyStore.getInstance(provider);
            theKeyStore.load(null, storePassword);

            this.theKeyStore = theKeyStore;
        } catch (KeyStoreException e) {
            throw DomainManagementLogger.ROOT_LOGGER.unableToStart(e);
        } catch (NoSuchAlgorithmException e) {
            throw DomainManagementLogger.ROOT_LOGGER.unableToStart(e);
        } catch (CertificateException e) {
            throw DomainManagementLogger.ROOT_LOGGER.unableToStart(e);
        } catch (IOException e) {
            throw DomainManagementLogger.ROOT_LOGGER.unableToStart(e);
        }

        super.start(context);
    }

    @Override
    public void stop(StopContext context) {
        super.stop(context);
        theKeyStore = null;
    }

    @Override
    protected KeyStore loadTrustStore() {
        return theKeyStore;
    }

}
