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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.arquillian.TestContainer;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.setuptask.OpenTelemetrySetupTask;

@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
@TestContainer(OpenTelemetryCollectorContainer.class)
public class BasicOpenTelemetryTestCase  {
    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap
                .create(WebArchive.class, BasicOpenTelemetryTestCase.class.getSimpleName() + ".war")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"/>"), "beans.xml");
    }

    @Inject
    private Tracer tracer;

    @Inject
    private OpenTelemetry openTelemetry;

    @Inject
    private Baggage baggage;


    @Test
    public void openTelemetryInjection() {
        Assert.assertNotNull("Injection of OpenTelemetry instance failed", openTelemetry);
    }

    @Test
    public void traceInjection() {
        Assert.assertNotNull("Injection of Tracer instance failed", tracer);
    }

    @Test
    public void baggageInjection() {
        Assert.assertNotNull("Injection of Baggage instance failed", baggage);
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
