/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry.api;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;

public class MicroProfileTelemetryCdiExtension implements Extension {
    private final Map<String, String> serverConfig;

    public MicroProfileTelemetryCdiExtension(Map<String, String> serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void registerOpenTelemetryConfigBean(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        abd.addBean()
                .scope(Singleton.class)
                .addQualifier(Default.Literal.INSTANCE)
                .types(OpenTelemetryConfig.class)
                .createWith(c -> {
                            Config appConfig = beanManager.createInstance().select(Config.class).get();
                            Map<String, String> properties = new HashMap<>(serverConfig);
                            // MicroProfile Telemetry is disabled by default
                            properties.put("otel.sdk.disabled", "true");
                            for (String propertyName : appConfig.getPropertyNames()) {
                                if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                                    appConfig.getOptionalValue(propertyName, String.class).ifPresent(
                                            value -> properties.put(propertyName, value));
                                }
                            }

                            return (OpenTelemetryConfig) () -> properties;
                        }
                );
    }
}
