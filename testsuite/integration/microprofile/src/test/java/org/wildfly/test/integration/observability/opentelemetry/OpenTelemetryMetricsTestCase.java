/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

@ServerSetup(OpenTelemetryWithCollectorSetupTask.class)
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
    @InSequence(1)
    public void metricsShouldStartPublishingImmediately() throws Exception {
        Assert.assertFalse("Metrics should be published immediately.",
            otelCollector.fetchMetrics("jvm_class_count").isEmpty());
    }

    @Test
    @InSequence(2)
    public void makeRequests() throws MalformedURLException {
        final String testName = "TeamCity";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(getDeploymentUrl(DEPLOYMENT_NAME) + "/metrics?name=" + testName);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Response response = target.request().get();
                Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                Assert.assertEquals("Hello, " + testName, response.readEntity(String.class));
            }
        }
    }

    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify there existence
    @Test
    @InSequence(3)
    public void getMetrics() throws InterruptedException {
        List<String> metricsToTest = List.of(OtelMetricResource.COUNTER_NAME);

        otelCollector.assertMetrics(prometheusMetrics -> metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(n)))));
    }
}
