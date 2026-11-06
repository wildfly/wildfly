/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry.api;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

public class MicroProfileTelemetryCdiExtension implements Extension {
    private final WildFlyOpenTelemetryConfig serverConfig;

    public MicroProfileTelemetryCdiExtension(WildFlyOpenTelemetryConfig config) {
        this.serverConfig = config;
    }

    public void registerOpenTelemetryConfigBean(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        abd.addBean()
                .scope(Singleton.class)
                .addQualifier(Default.Literal.INSTANCE)
                .types(WildFlyOpenTelemetryConfig.class)
                .createWith(c -> {
                            Config appConfig = beanManager.createInstance().select(Config.class).get();
                            Map<String, String> properties = new HashMap<>(serverConfig.getProperties());
                            properties.put("otel.sdk.disabled", "true");
                            for (String propertyName : appConfig.getPropertyNames()) {
                                if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                                    appConfig.getOptionalValue(propertyName, String.class).ifPresent(
                                            value -> properties.put(propertyName, value));
                                }
                            }

                            return serverConfig.withProperties(properties);
                        }
                );
    }
}
