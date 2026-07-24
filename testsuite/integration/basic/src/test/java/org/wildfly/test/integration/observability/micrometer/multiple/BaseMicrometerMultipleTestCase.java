/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.multiple;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@TestcontainersRequired
@RunAsClient
public abstract class BaseMicrometerMultipleTestCase {
    protected static final String SERVICE_ONE = "service-one";
    protected static final String SERVICE_TWO = "service-two";
    protected static final int REQUEST_COUNT = 5;

    @Testcontainer
    protected OpenTelemetryCollectorContainer otelCollector;

    protected void makeRequests(URI service) {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(service);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Assert.assertEquals(200, target.request().get().getStatus());
            }
        }
    }
}
