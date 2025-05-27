/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private static final String ZSTD_COMPRESSION = "zstd";
    private static final Set<String> COMPRESSION_TYPES = Set.of(SNAPPY_COMPRESSION, ZSTD_COMPRESSION);

    // Whether to disable Snappy and zstd-jni on Windows and Mac
    public static final boolean DISABLE_NATIVE_COMPRESSION_ON_WINDOWS_AND_MAC = false;

    @Override
    public Map<String, Object> customize(String channel, Config channelConfig, Map<String, Object> config) {
        if (DISABLE_NATIVE_COMPRESSION_ON_WINDOWS_AND_MAC) {
            String os = WildFlySecurityManager.getPropertyPrivileged("os.name", "x").toLowerCase(Locale.ENGLISH);
            boolean runningOnWindowsOrMac = os.startsWith("windows") || os.startsWith("mac os");
            if (runningOnWindowsOrMac) {
                Object compressionValue = config.get(COMPRESSION_TYPE_PROPERTY);
                if (compressionValue != null && COMPRESSION_TYPES.contains(compressionValue)) {
                    throw MicroProfileReactiveMessagingKafkaLogger.LOGGER.compressionNotSupportedOnWindows(
                            compressionValue.toString(), COMPRESSION_TYPE_PROPERTY);
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
