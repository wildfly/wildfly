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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service to handle the creation of the TrustManager[].
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class AbstractTrustManagerService implements Service<TrustManager[]> {

    private volatile TrustManager[] theTrustManagers;

    /*
     * Service Lifecycle Methods
     */

    @Override
    public void start(StartContext context) throws StartException {
        try {
            theTrustManagers = createTrustManagers();
        } catch (NoSuchAlgorithmException e) {
            throw DomainManagementLogger.ROOT_LOGGER.unableToStart(e);
        } catch (KeyStoreException e) {
            throw DomainManagementLogger.ROOT_LOGGER.unableToStart(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        this.theTrustManagers = null;
    }

    /*
     * Value Method
     */

    @Override
    public TrustManager[] getValue() throws IllegalStateException, IllegalArgumentException {
        return theTrustManagers;
    }

    /**
     * Method to create the TrustManager[]
     *
     * This method returns the created TrustManager[] so that sub classes can have the opportunity to either wrap or replace
     * this call.
     *
     * @return The TrustManager[] based on the supplied {@link KeyStore}
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    protected TrustManager[] createTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        KeyStore trustStore = loadTrustStore();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        return trustManagerFactory.getTrustManagers();
    }

    protected abstract KeyStore loadTrustStore();

    static final class ServiceUtil {

        private static final String SERVICE_SUFFIX = "trust-manager";

        public static ServiceName createServiceName(final ServiceName parentService) {
            return parentService.append(SERVICE_SUFFIX);
        }

        public static ServiceBuilder<?> addDependency(final ServiceBuilder<?> sb, final Injector<TrustManager[]> injector, final ServiceName parentService) {
            sb.addDependency(createServiceName(parentService), TrustManager[].class, injector);

            return sb;
        }

    }

}
