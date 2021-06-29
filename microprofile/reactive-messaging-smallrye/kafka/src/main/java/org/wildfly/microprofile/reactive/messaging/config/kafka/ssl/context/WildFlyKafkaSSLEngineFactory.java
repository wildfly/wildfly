/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import static org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context.ReactiveMessagingSslConfigProcessor.SSL_CONTEXT_PROPERTY_SUFFIX;
import static org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private.MicroProfileReactiveMessagingKafkaLogger.LOGGER;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WildFlyKafkaSSLEngineFactory implements org.apache.kafka.common.security.auth.SslEngineFactory {
    private volatile SSLContext sslContext;

    @Override
    public void configure(Map<String, ?> configs) {
        SSLContext context = ElytronSSLContextRegistry.getInstalledSSLContext((String) configs.get(SSL_CONTEXT_PROPERTY_SUFFIX));
        if (context == null) {
            throw LOGGER.noElytronClientSSLContext((String) configs.get(SSL_CONTEXT_PROPERTY_SUFFIX));
        }
        sslContext = context;
    }

    @Override
    public SSLEngine createClientSslEngine(String peerHost, int peerPort, String endpointIdentification) {
        // Code copied and adjusted from Kafka
        SSLEngine sslEngine = sslContext.createSSLEngine(peerHost, peerPort);

        sslEngine.setUseClientMode(true);
        SSLParameters sslParams = sslEngine.getSSLParameters();
        // SSLParameters#setEndpointIdentificationAlgorithm enables endpoint validation
        // only in client mode. Hence, validation is enabled only for clients.

        // Hard code this to https for now
        // This is the default value for SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG
        // which is documented as setting it to an empty string if we don't want host name verification.
        // There is a 'Host name verification' subsection under https://kafka.apache.org/documentation/#security_ssl
        // which explains it in more detail
        sslParams.setEndpointIdentificationAlgorithm("https");
        sslEngine.setSSLParameters(sslParams);
        return sslEngine;
    }

    @Override
    public SSLEngine createServerSslEngine(String peerHost, int peerPort) {
        // We are only dealing with clients
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldBeRebuilt(Map<String, Object> nextConfigs) {
        return false;
    }

    @Override
    public Set<String> reconfigurableConfigs() {
        return Collections.emptySet();
    }

    @Override
    public KeyStore keystore() {
        // We are only dealing with clients
        // This only comes into play during reconfiguration. Returning null is ok
        return null;
    }

    @Override
    public KeyStore truststore() {
        // This only comes into play during reconfiguration. Returning null is ok
        return null;
    }

    @Override
    public void close() throws IOException {
        this.sslContext = null;
    }

}
