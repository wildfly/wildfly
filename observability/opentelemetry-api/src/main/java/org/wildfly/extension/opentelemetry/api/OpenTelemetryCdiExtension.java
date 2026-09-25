/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import io.opentelemetry.api.OpenTelemetry;
import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryProducer;
import io.smallrye.opentelemetry.implementation.rest.OpenTelemetryClientFilter;
import io.smallrye.opentelemetry.implementation.rest.OpenTelemetryServerFilter;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.inject.Singleton;

/**
 * Installs WildFly's deployment-scoped OpenTelemetry instance and producers into CDI while replacing SmallRye's
 * independently configured producer.
 */
public final class OpenTelemetryCdiExtension implements Extension {
    private final OpenTelemetry openTelemetry;

    /**
     * Creates the CDI extension for a deployment-specific OpenTelemetry view.
     *
     * @param openTelemetry the OpenTelemetry instance exposed to the deployment
     */
    public OpenTelemetryCdiExtension(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Registers the REST filters and WildFly producer before CDI discovers application beans.
     *
     * @param beforeBeanDiscovery the CDI lifecycle event
     * @param beanManager the deployment bean manager
     */
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(OpenTelemetryServerFilter.class),
                OpenTelemetryServerFilter.class.getName());
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(OpenTelemetryClientFilter.class),
                OpenTelemetryClientFilter.class.getName());
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(WildFlyOpenTelemetryProducer.class),
                WildFlyOpenTelemetryProducer.class.getName());
    }

    /**
     * Vetoes SmallRye's producer so it cannot create a second SDK for the deployment.
     *
     * @param event the producer discovery event
     */
    public void vetoSmallRyeOpenTelemetryProducer(@Observes
                                                  ProcessAnnotatedType<OpenTelemetryProducer> event) {
        event.veto();
    }

    /**
     * Registers the deployment OpenTelemetry instance after CDI has discovered application beans.
     *
     * @param abd the CDI lifecycle event used to add the synthetic bean
     */
    public void registerOpenTelemetryBeans(@Observes AfterBeanDiscovery abd) {
        // Register the OpenTelemetry instance - WildFlyOpenTelemetryProducer will use this
        // to produce Tracer, Meter, Span, and Baggage
        abd.addBean()
                .scope(Singleton.class)
                .addQualifier(Default.Literal.INSTANCE)
                .types(OpenTelemetry.class)
                .createWith(e -> openTelemetry);
    }
}
