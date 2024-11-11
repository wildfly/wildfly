/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.MapUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

@ServerSetup(OpenTelemetrySetupTask.class)
@RunAsClient
public class OpenTelemetryMetricsTestCase extends BaseOpenTelemetryTest {
    private static final int REQUEST_COUNT = 5;
    private static final String DEPLOYMENT_NAME = "otel-metrics-test";

    @Deployment
    public static Archive<?> getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify there existence
    @Test
    @InSequence(1)
    public void metricsShouldStartPublishingImmediately() throws Exception {
        otelCollector.assertMetrics(metrics -> {
                try {
                    verifyMetricsPresent(metrics, List.of("jvm_class_count"));
                } catch (Exception e) {
                    Assert.fail("Metrics should be published immediately.");
                }
            }
        );
    }

    @Test
    @InSequence(2)
    public void makeRequests() throws MalformedURLException {
        final String testName = "WildFly";
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
        otelCollector.assertMetrics(prometheusMetrics -> {
                // Modify metric name to reflect the Prometheus format for counters
                verifyMetricsPresent(prometheusMetrics, List.of(OtelMetricResource.COUNTER_NAME ));
            }
        );
    }

    private void verifyMetricsPresent(List<PrometheusMetric> prometheusMetrics,
                                      List<String> metricsToTest) {
        metricsToTest.forEach(name -> {
                String sanitizedName = PrometheusMetric.sanitizeMetricName(name);
                // We check that a key "startsWith" as Prometheus-formatted metrics have suffixes added based
                // on the metric type
                Assert.assertTrue("Missing metric: " + name,
                    prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(sanitizedName)));
            }
        );
    }

    public Map<String, PrometheusMetric> convertListWithApacheCommons(List<PrometheusMetric> list) {
        Map<String, PrometheusMetric> map = new HashMap<>();
        MapUtils.populateMap(map, list, PrometheusMetric::getKey);
        return map;
    }
}
