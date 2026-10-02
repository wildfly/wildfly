/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

@ExtendWith(ArquillianExtension.class)
@ServerSetup(OpenTelemetrySetupTask.class)
@TestcontainersRequired
public class BasicOpenTelemetryTestCase {
    @Inject
    private Tracer tracer;

    @Inject
    private OpenTelemetry openTelemetry;

    @Inject
    private Baggage baggage;

    @Inject
    private Meter meter;

    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, "basic-otel.war")
            .addClasses(
                JaxRsActivator.class,
                OtelMetricResource.class
            )
            .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    void openTelemetryInjection() {
        assertNotNull(openTelemetry, "Injection of OpenTelemetry instance failed");
    }

    @Test
    void traceInjection() {
        assertNotNull(tracer, "Injection of Tracer instance failed");
    }

    @Test
    void baggageInjection() {
        assertNotNull(baggage, "Injection of Baggage instance failed");
    }

    @Test
    void meterInjection() {
        assertNotNull(meter, "Injection of Meter instance failed");
    }

    @Test
    void restClientHasFilterAdded() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            assertTrue(
                    client.getConfiguration()
                            .isRegistered(Class.forName("io.smallrye.opentelemetry.implementation.rest.OpenTelemetryClientFilter"))
            );
        }
    }
}
