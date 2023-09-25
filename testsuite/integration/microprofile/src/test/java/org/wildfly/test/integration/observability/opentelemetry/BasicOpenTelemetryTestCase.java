/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
public class BasicOpenTelemetryTestCase extends BaseOpenTelemetryTest {
    @Inject
    private Tracer tracer;

    @Inject
    private OpenTelemetry openTelemetry;

    @Inject
    private Baggage baggage;

    @Deployment
    public static Archive getDeployment() {
        return buildBaseArchive(BasicOpenTelemetryTestCase.class.getSimpleName());
    }

    @Test
    public void openTelemetryInjection() {
        Assert.assertNotNull(openTelemetry);
    }

    @Test
    public void traceInjection() {
        Assert.assertNotNull(tracer);
    }

    @Test
    public void baggageInjection() {
        Assert.assertNotNull(baggage);
    }

    @Test
    public void restClientHasFilterAdded() throws ClassNotFoundException {
        try (Client client = ClientBuilder.newClient()) {
            Assert.assertTrue(
                    client.getConfiguration().isRegistered(
                            Class.forName("io.smallrye.opentelemetry.implementation.rest.OpenTelemetryClientFilter"))
            );
        }
    }
}
