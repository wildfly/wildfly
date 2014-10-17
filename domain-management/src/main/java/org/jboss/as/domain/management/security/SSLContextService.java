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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service to handle the creation of a single SSLContext based on the injected key and trust managers.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SSLContextService implements Service<SSLContext> {

    private InjectedValue<KeyManager[]> injectedKeyManagers = new InjectedValue<KeyManager[]>();
    private InjectedValue<TrustManager[]> injectedtrustManagers = new InjectedValue<TrustManager[]>();

    private volatile String protocol;
    private volatile Set<String> enabledCipherSuites;
    private volatile Set<String> enabledProtocols;
    private volatile SSLContext theSSLContext;

    SSLContextService(final String protocol, final Set<String> enabledCipherSuites, final Set<String> enabledProtocols) {
        this.protocol = protocol;
        this.enabledCipherSuites = enabledCipherSuites;
        this.enabledProtocols = enabledProtocols;
    }

    public String getProtocol() {
        return protocol;
    }



    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /*
     * Service Lifecycle Methods
     */

    @Override
    public void start(final StartContext context) throws StartException {
        KeyManager[] keyManagers = injectedKeyManagers.getOptionalValue();
        TrustManager[] trustManagers = injectedtrustManagers.getOptionalValue();

        try {
            SSLContext sslContext = SSLContext.getInstance(protocol);
            sslContext.init(keyManagers, trustManagers, null);

            if (enabledCipherSuites.isEmpty() != false || enabledProtocols.isEmpty() != false) {
                SSLParameters parameters = sslContext.getSupportedSSLParameters();

                String[] commonCiphers;
                if (enabledCipherSuites.isEmpty()) {
                    commonCiphers = new String[0];
                } else {
                    commonCiphers = calculateCommon(parameters.getCipherSuites(), enabledCipherSuites);
                    // Not valid to be empty now as there was an attempt to find a common set.
                    if (commonCiphers.length == 0) {
                        throw MESSAGES.noCipherSuitesInCommon(
                                Arrays.asList(parameters.getCipherSuites()).toString(), enabledCipherSuites.toString());
                    }
                }

                String[] commonProtocols;
                if (enabledProtocols.isEmpty()) {
                    commonProtocols = new String[0];
                } else {
                    commonProtocols = calculateCommon(parameters.getProtocols(), enabledProtocols);
                    // Not valid to be empty now as there was an attempt to find a common set.
                    if (commonProtocols.length == 0) {
                        throw MESSAGES.noProtocolsInCommon(Arrays.asList(parameters.getProtocols())
                                .toString(), enabledProtocols.toString());
                    }
                }

                sslContext = new WrapperSSLContext(sslContext, commonCiphers, commonProtocols);
            }

            this.theSSLContext = sslContext;

        } catch (NoSuchAlgorithmException e) {
            throw MESSAGES.unableToStart(e);
        } catch (KeyManagementException e) {
            throw MESSAGES.unableToStart(e);
        }
    }

    private String[] calculateCommon(String[] supported, Set<String> configured) {
        ArrayList<String> matched = new ArrayList<String>();
        for (String current : supported) {
            if (configured.contains(current)) {
                matched.add(current);
            }
        }

        return matched.toArray(new String[matched.size()]);
    }


    @Override
    public void stop(final StopContext context) {
        theSSLContext = null;
    }

    /*
     * Value factory method.
     */

    @Override
    public SSLContext getValue() throws IllegalStateException, IllegalArgumentException {
        return theSSLContext;
    }

    /*
     * Injector Accessor Methods
     */

    public InjectedValue<KeyManager[]> getKeyManagerInjector() {
        return injectedKeyManagers;
    }

    public InjectedValue<TrustManager[]> getTrustManagerInjector() {
        return injectedtrustManagers;
    }

    public static final class ServiceUtil {

        private static final String SERVICE_SUFFIX = "ssl-context";
        private static final String TRUST_ONLY_SERVICE_SUFFIX = SERVICE_SUFFIX + "-trust-only";

        public static ServiceName createServiceName(final ServiceName parentService, final boolean trustOnly) {
            return parentService.append(trustOnly ? TRUST_ONLY_SERVICE_SUFFIX : SERVICE_SUFFIX);
        }

        public static ServiceBuilder<?> addDependency(final ServiceBuilder<?> sb, final Injector<SSLContext> injector, final ServiceName parentService, final boolean trustOnly) {
            sb.addDependency(createServiceName(parentService, trustOnly), SSLContext.class, injector);

            return sb;
        }

    }
}
