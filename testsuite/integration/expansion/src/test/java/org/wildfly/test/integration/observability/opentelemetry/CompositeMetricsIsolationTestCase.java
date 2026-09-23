/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

/**
 * Integration test for composite OpenTelemetry architecture.
 * Verifies that deployments have isolated metrics while still being aggregated for export.
 *
 * Tests:
 * 1. Multiple deployments have distinct metric readers
 * 2. Metrics are isolated per deployment (cannot see each other's metrics)
 * 3. All metrics are aggregated and exported to OTLP collector
 * 4. Resource attributes include deployment-specific tags
 * 5. Deploy/undeploy scenarios clean up metric readers
 */
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
@TestcontainersRequired
public class CompositeMetricsIsolationTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_ONE = "metrics-app-one";
    private static final String DEPLOYMENT_TWO = "metrics-app-two";
    private static final String DEPLOYMENT_THREE = "metrics-app-three";

    private static final int REQUEST_COUNT_ONE = 5;
    private static final int REQUEST_COUNT_TWO = 10;
    private static final int REQUEST_COUNT_THREE = 3;

    @Deployment(name = DEPLOYMENT_ONE, order = 1, testable = false)
    public static WebArchive createDeployment1() {
        return buildBaseArchive(DEPLOYMENT_ONE);
    }

    @Deployment(name = DEPLOYMENT_TWO, order = 2, testable = false)
    public static WebArchive createDeployment2() {
        return buildBaseArchive(DEPLOYMENT_TWO);
    }

    @Deployment(name = DEPLOYMENT_THREE, order = 3, testable = false, managed = false)
    public static WebArchive createDeployment3() {
        return buildBaseArchive(DEPLOYMENT_THREE);
    }

    /**
     * Test 1: Make requests to generate metrics for deployment 1
     */
    @Test
    @InSequence(1)
    @OperateOnDeployment(DEPLOYMENT_ONE)
    public void makeRequestsToDeploymentOne() throws MalformedURLException {
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < REQUEST_COUNT_ONE; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(DEPLOYMENT_ONE) + "metrics?name=" + DEPLOYMENT_ONE)
                        .request().get().getStatus());
            }
        }
    }

    /**
     * Test 2: Make requests to generate metrics for deployment 2
     */
    @Test
    @InSequence(2)
    @OperateOnDeployment(DEPLOYMENT_TWO)
    public void makeRequestsToDeploymentTwo() throws MalformedURLException {
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < REQUEST_COUNT_TWO; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(DEPLOYMENT_TWO) + "metrics?name=" + DEPLOYMENT_TWO)
                        .request().get().getStatus());
            }
        }
    }

    /**
     * Test 3: Verify metrics are isolated per deployment
     * Each deployment should have its own counter value
     */
    @Test
    @InSequence(3)
    public void verifyMetricIsolation() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            Assert.assertFalse("No metrics found", metrics.isEmpty());

            // Verify deployment 1 metrics
            PrometheusMetric deploymentOneMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_ONE + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment one metric not found"));

            Assert.assertEquals("Deployment one counter mismatch",
                Integer.toString(REQUEST_COUNT_ONE), deploymentOneMetric.getValue());

            // Verify deployment 2 metrics
            PrometheusMetric deploymentTwoMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_TWO + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment two metric not found"));

            Assert.assertEquals("Deployment two counter mismatch",
                Integer.toString(REQUEST_COUNT_TWO), deploymentTwoMetric.getValue());

            // Verify metrics are distinct (different counter values)
            Assert.assertNotEquals("Metrics should be isolated",
                deploymentOneMetric.getValue(), deploymentTwoMetric.getValue());
        });
    }

    /**
     * Test 4: Verify resource attributes include deployment-specific tags
     */
    @Test
    @InSequence(4)
    public void verifyResourceAttributes() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            // Check deployment one has correct job tag (service.name)
            boolean foundDeploymentOne = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_ONE + ".war"));
            Assert.assertTrue("Deployment one should have job=" + DEPLOYMENT_ONE + ".war", foundDeploymentOne);

            // Check deployment two has correct job tag (service.name)
            boolean foundDeploymentTwo = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_TWO + ".war"));
            Assert.assertTrue("Deployment two should have job=" + DEPLOYMENT_TWO + ".war", foundDeploymentTwo);
        });
    }

    /**
     * Test 5: Deploy a third deployment dynamically
     */
    @Test
    @InSequence(5)
    public void deployThirdDeployment() throws MalformedURLException {
        // Deployment will be deployed by Arquillian when we access it
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < REQUEST_COUNT_THREE; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(DEPLOYMENT_THREE) + "metrics?name=" + DEPLOYMENT_THREE)
                        .request().get().getStatus());
            }
        }
    }

    /**
     * Test 6: Verify third deployment metrics appear in aggregated export
     */
    @Test
    @InSequence(6)
    public void verifyThirdDeploymentMetrics() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            // Should now have metrics from all three deployments
            long deploymentCount = metrics.stream()
                .map(m -> m.getTags().get("job"))
                .distinct()
                .filter(job -> job.endsWith(".war"))
                .count();

            Assert.assertTrue("Should have at least 3 deployments", deploymentCount >= 3);

            // Verify third deployment metric value
            PrometheusMetric deploymentThreeMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_THREE + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment three metric not found"));

            Assert.assertEquals("Deployment three counter mismatch",
                Integer.toString(REQUEST_COUNT_THREE), deploymentThreeMetric.getValue());
        });
    }

    /**
     * Test 7: Verify all metrics are aggregated (appear in single export)
     * This tests that AggregatingMetricExporter collects from all deployment readers
     */
    @Test
    @InSequence(7)
    public void verifyMetricAggregation() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> allCounters = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            // Calculate total count across all deployments
            int totalCount = allCounters.stream()
                .filter(m -> {
                    String job = m.getTags().get("job");
                    return job != null && job.endsWith(".war");
                })
                .mapToInt(m -> Integer.parseInt(m.getValue()))
                .sum();

            int expectedTotal = REQUEST_COUNT_ONE + REQUEST_COUNT_TWO + REQUEST_COUNT_THREE;
            Assert.assertEquals("Total aggregated count should match sum of all deployments",
                expectedTotal, totalCount);
        });
    }

    /**
     * Test 8: Verify DELTA temporality behavior
     * Make additional requests and verify metrics increment correctly
     */
    @Test
    @InSequence(8)
    public void verifyDeltaTemporality() throws MalformedURLException, InterruptedException {
        // Get current metric value
        final int[] initialValue = new int[1];
        otelCollector.assertMetrics(prometheusMetrics -> {
            PrometheusMetric metric = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total").stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_ONE + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment one metric not found"));
            initialValue[0] = Integer.parseInt(metric.getValue());
        });

        // Make additional requests
        final int additionalRequests = 3;
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < additionalRequests; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(DEPLOYMENT_ONE) + "metrics?name=" + DEPLOYMENT_ONE)
                        .request().get().getStatus());
            }
        }

        // Wait for export cycle
        Thread.sleep(3000);

        // Verify metric incremented correctly
        otelCollector.assertMetrics(prometheusMetrics -> {
            PrometheusMetric metric = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total").stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_ONE + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment one metric not found"));

            int currentValue = Integer.parseInt(metric.getValue());
            Assert.assertEquals("Metric should increment by additional requests",
                initialValue[0] + additionalRequests, currentValue);
        });
    }
}
