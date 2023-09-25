/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import java.util.Map;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import io.smallrye.opentelemetry.implementation.rest.OpenTelemetryClientFilter;
import io.smallrye.opentelemetry.implementation.rest.OpenTelemetryServerFilter;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;

public final class OpenTelemetryCdiExtension implements Extension {
    private final boolean useServerConfig;
    private final Map<String, String> config;

    public OpenTelemetryCdiExtension(boolean useServerConfig, Map<String, String> config) {
        this.useServerConfig = useServerConfig;
        this.config = config;
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(OpenTelemetryServerFilter.class),
                OpenTelemetryServerFilter.class.getName());
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(OpenTelemetryClientFilter.class),
                OpenTelemetryClientFilter.class.getName());
    }

    public void registerOpenTelemetryConfigBean(@Observes AfterBeanDiscovery abd) {
        if (useServerConfig) {
            abd.addBean()
                    .scope(Singleton.class)
                    .addQualifier(Default.Literal.INSTANCE)
                    .types(OpenTelemetryConfig.class)
                    .createWith(e -> (OpenTelemetryConfig) () -> config);
        }
    }
}
