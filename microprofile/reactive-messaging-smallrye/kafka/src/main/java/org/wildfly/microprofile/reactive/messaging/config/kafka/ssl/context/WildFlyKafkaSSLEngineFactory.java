/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import static org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private.MicroProfileReactiveMessagingKafkaLogger.LOGGER;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.wildfly.microprofile.reactive.messaging.common.security.ElytronSSLContextRegistry;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WildFlyKafkaSSLEngineFactory implements org.apache.kafka.common.security.auth.SslEngineFactory {
    static final String SSL_ENGINE_FACTORY_CLASS = "ssl.engine.factory.class";
    private volatile SSLContext sslContext;

    @Override
    public void configure(Map<String, ?> configs) {
        // Only the suffix will have been passed through the Kafka connection config mechanisms
        SSLContext context = ElytronSSLContextRegistry.getInstalledSSLContext(
                (String) configs.get(ElytronSSLContextRegistry.SSL_CONTEXT_PROPERTY));
        if (context == null) {
            throw LOGGER.noElytronClientSSLContext((String) configs.get(ElytronSSLContextRegistry.SSL_CONTEXT_PROPERTY));
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
