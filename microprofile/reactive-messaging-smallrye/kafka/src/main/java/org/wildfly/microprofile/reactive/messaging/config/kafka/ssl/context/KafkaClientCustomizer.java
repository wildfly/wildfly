/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import java.util.Locale;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.reactive.messaging.ClientCustomizer;
import org.eclipse.microprofile.config.Config;
import org.wildfly.microprofile.reactive.messaging.common.security.ElytronSSLContextRegistry;
import org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private.MicroProfileReactiveMessagingKafkaLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

@ApplicationScoped
public class KafkaClientCustomizer implements ClientCustomizer<Map<String, Object>> {
    private static final String COMPRESSION_TYPE_PROPERTY = "compression.type";
    private static final String SNAPPY_COMPRESSION = "snappy";

    // Whether to disable Snappy on Windows and Mac
    private static final boolean DISABLE_SNAPPY_ON_WINDOWS_AND_MAC = false;

    @Override
    public Map<String, Object> customize(String channel, Config channelConfig, Map<String, Object> config) {
        if (DISABLE_SNAPPY_ON_WINDOWS_AND_MAC) {
            String os = WildFlySecurityManager.getPropertyPrivileged("os.name", "x").toLowerCase(Locale.ENGLISH);
            boolean runningOnWindowsOrMac = os.startsWith("windows") || os.startsWith("mac os");
            if (runningOnWindowsOrMac) {
                if (config.containsKey(COMPRESSION_TYPE_PROPERTY) && config.get(COMPRESSION_TYPE_PROPERTY).equals(SNAPPY_COMPRESSION)) {
                    throw MicroProfileReactiveMessagingKafkaLogger.LOGGER.snappyCompressionNotSupportedOnWindows(COMPRESSION_TYPE_PROPERTY);
                }
            }
        }

        if (config.containsKey(ElytronSSLContextRegistry.SSL_CONTEXT_PROPERTY)) {
            MicroProfileReactiveMessagingKafkaLogger.LOGGER
                    .foundPropertyUsingElytronClientSSLContext(
                            ElytronSSLContextRegistry.SSL_CONTEXT_PROPERTY,
                            (String) config.get(ElytronSSLContextRegistry.SSL_CONTEXT_PROPERTY));
            config.put(WildFlyKafkaSSLEngineFactory.SSL_ENGINE_FACTORY_CLASS, WildFlyKafkaSSLEngineFactory.class);
        }

        return config;
    }
}
