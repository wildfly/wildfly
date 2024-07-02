/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.multiple;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.arquillian.TestContainer;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.container.PrometheusMetric;
import org.wildfly.test.integration.observability.setuptask.MicrometerSetupTask;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@RunAsClient
@TestContainer(OpenTelemetryCollectorContainer.class)
public abstract class BaseMultipleTestCase {
    protected static final String SERVICE_ONE = "service-one";
    protected static final String SERVICE_TWO = "service-two";
    protected static final int REQUEST_COUNT = 5;

    @ArquillianResource
    protected OpenTelemetryCollectorContainer otelContainer;

    protected void makeRequests(URI service) throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(service);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Assert.assertEquals(200, target.request().get().getStatus());
            }
        }
    }

    protected @NotNull List<PrometheusMetric> getMetricsByName(List<PrometheusMetric> metrics, String key) {
        return metrics.stream()
                .filter(m -> m.getKey().equals(key))
                .collect(Collectors.toList());
    }
}
