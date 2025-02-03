/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import io.opentelemetry.api.OpenTelemetry;
import io.smallrye.opentelemetry.api.HttpClientOptionsConsumer;
import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import io.smallrye.opentelemetry.implementation.rest.OpenTelemetryClientFilter;
import io.smallrye.opentelemetry.implementation.rest.OpenTelemetryServerFilter;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;

public final class OpenTelemetryCdiExtension implements Extension {
    private final boolean useServerConfig;
    private final WildFlyOpenTelemetryConfig config;

    public OpenTelemetryCdiExtension(boolean useServerConfig, WildFlyOpenTelemetryConfig config) {
        this.useServerConfig = useServerConfig;
        this.config = config;
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(OpenTelemetryServerFilter.class),
                OpenTelemetryServerFilter.class.getName());
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(OpenTelemetryClientFilter.class),
                OpenTelemetryClientFilter.class.getName());
    }

    // For the publication of metrics to begin, the OpenTelemetry instance must be created. Unless something like
    // this is done, that will not happen until the first request to the application (via the OpenTelemetryServerFilter).
    // Forcing the creation of that instance here causes that publication to begin as soon as the application is
    // deployed/started. See https://issues.redhat.com/browse/WFLY-20075
    void forceEagerInstantiationOfOpenTelemetry(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        beanManager.createInstance().select(OpenTelemetry.class).get();
    }

    public void registerOpenTelemetryBeans(@Observes AfterBeanDiscovery abd) {
        if (useServerConfig) {
            abd.addBean()
                    .scope(Singleton.class)
                    .addQualifier(Default.Literal.INSTANCE)
                    .types(OpenTelemetryConfig.class)
                    .createWith(e -> config);
        }
        if (config.getSslContext() != null) {
            abd.addBean()
                .scope(Singleton.class)
                .addQualifier(Default.Literal.INSTANCE)
                .types(HttpClientOptionsConsumer.class)
                .createWith(e -> new WildFlyHttpClientOptionsConsumer(config));
        }
    }
}
