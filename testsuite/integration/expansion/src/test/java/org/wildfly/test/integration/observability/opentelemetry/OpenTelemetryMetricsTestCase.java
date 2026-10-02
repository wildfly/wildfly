/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

@ServerSetup(OpenTelemetryWithCollectorSetupTask.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@RunAsClient
public class OpenTelemetryMetricsTestCase extends BaseOpenTelemetryTest {
    private static final int REQUEST_COUNT = 5;
    private static final String DEPLOYMENT_NAME = "otel-metrics-test";

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    @Test
    @Order(1)
    void metricsShouldStartPublishingImmediately() throws Exception {
        assertFalse(otelCollector.fetchMetrics("jvm_class_count").isEmpty(),
            "Metrics should be published immediately.");
    }

    @Test
    @Order(2)
    void makeRequests() throws Exception {
        final String testName = "TeamCity";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(getDeploymentUrl(DEPLOYMENT_NAME) + "/metrics?name=" + testName);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Response response = target.request().get();
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertEquals("Hello, " + testName, response.readEntity(String.class));
            }
        }
    }

    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify their existence
    @Test
    @Order(3)
    void getMetrics() throws Exception {
        List<String> metricsToTest = List.of(OtelMetricResource.COUNTER_NAME);

        otelCollector.assertMetrics(prometheusMetrics -> metricsToTest.forEach(n -> assertTrue(prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(n)),
                "Missing metric: " + n)));
    }
}
