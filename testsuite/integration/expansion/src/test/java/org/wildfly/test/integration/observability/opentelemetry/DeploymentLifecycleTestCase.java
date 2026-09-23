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
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

/**
 * Tests deployment lifecycle scenarios for OpenTelemetry composite architecture.
 *
 * Tests:
 * 1. Metrics persist across deployment lifecycle
 * 2. Undeploy removes deployment from metric aggregation
 * 3. Redeploy creates new metric reader (no collision)
 * 4. Server continues exporting remaining deployment metrics after undeploy
 */
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
@TestcontainersRequired
public class DeploymentLifecycleTestCase extends BaseOpenTelemetryTest {
    private static final String PERSISTENT_DEPLOYMENT = "lifecycle-persistent";
    private static final String DYNAMIC_DEPLOYMENT = "lifecycle-dynamic";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = PERSISTENT_DEPLOYMENT, order = 1, testable = false)
    public static WebArchive createPersistentDeployment() {
        return buildBaseArchive(PERSISTENT_DEPLOYMENT);
    }

    @Deployment(name = DYNAMIC_DEPLOYMENT, order = 2, testable = false, managed = false)
    public static WebArchive createDynamicDeployment() {
        return buildBaseArchive(DYNAMIC_DEPLOYMENT);
    }

    /**
     * Test 1: Generate baseline metrics for persistent deployment
     */
    @Test
    @InSequence(1)
    public void generatePersistentDeploymentMetrics() throws MalformedURLException {
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < 5; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(PERSISTENT_DEPLOYMENT) + "metrics?name=test")
                        .request().get().getStatus());
            }
        }
    }

    /**
     * Test 2: Verify persistent deployment metrics are exported
     */
    @Test
    @InSequence(2)
    public void verifyPersistentDeploymentMetrics() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            boolean found = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), PERSISTENT_DEPLOYMENT + ".war"));

            Assert.assertTrue("Persistent deployment metrics should be present", found);
        });
    }

    /**
     * Test 3: Deploy dynamic deployment
     */
    @Test
    @InSequence(3)
    public void deployDynamicDeployment() throws MalformedURLException {
        deployer.deploy(DYNAMIC_DEPLOYMENT);

        // Generate metrics
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < 7; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(DYNAMIC_DEPLOYMENT) + "metrics?name=test")
                        .request().get().getStatus());
            }
        }
    }

    /**
     * Test 4: Verify both deployments are present in metrics
     */
    @Test
    @InSequence(4)
    public void verifyBothDeploymentsPresent() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            long deploymentCount = metrics.stream()
                .map(m -> m.getTags().get("job"))
                .filter(job -> job.equals(PERSISTENT_DEPLOYMENT + ".war") ||
                               job.equals(DYNAMIC_DEPLOYMENT + ".war"))
                .distinct()
                .count();

            Assert.assertEquals("Should have metrics from both deployments", 2, deploymentCount);
        });
    }

    /**
     * Test 5: Undeploy dynamic deployment
     */
    @Test
    @InSequence(5)
    public void undeployDynamicDeployment() throws InterruptedException {
        deployer.undeploy(DYNAMIC_DEPLOYMENT);

        // Wait for next export cycle to ensure undeploy processed
        Thread.sleep(3000);
    }

    /**
     * Test 6: Verify dynamic deployment metrics no longer exported
     * but persistent deployment metrics still present
     */
    @Test
    @InSequence(6)
    public void verifyDynamicDeploymentRemoved() throws InterruptedException, MalformedURLException {
        // Generate more metrics on persistent deployment to trigger export
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < 2; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(PERSISTENT_DEPLOYMENT) + "metrics?name=test")
                        .request().get().getStatus());
            }
        }

        // Wait for export
        Thread.sleep(3000);

        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            // Persistent deployment should still be present
            boolean persistentFound = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), PERSISTENT_DEPLOYMENT + ".war"));
            Assert.assertTrue("Persistent deployment metrics should still be present", persistentFound);

            // Dynamic deployment metrics might still appear in last export due to timing,
            // but should have the old value (7) not increasing
            List<PrometheusMetric> dynamicMetrics = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DYNAMIC_DEPLOYMENT + ".war"))
                .toList();

            if (!dynamicMetrics.isEmpty()) {
                // If present, should still be 7 (not increasing)
                Assert.assertEquals("Dynamic deployment counter should not increase after undeploy",
                    "7", dynamicMetrics.get(0).getValue());
            }
        });
    }

    /**
     * Test 7: Redeploy dynamic deployment
     * Verifies no collision with previous deployment's metric reader
     */
    @Test
    @InSequence(7)
    public void redeployDynamicDeployment() throws MalformedURLException, InterruptedException {
        deployer.deploy(DYNAMIC_DEPLOYMENT);

        // Wait for deployment to be ready
        Thread.sleep(2000);

        // Generate metrics (should start fresh)
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < 3; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(DYNAMIC_DEPLOYMENT) + "metrics?name=test")
                        .request().get().getStatus());
            }
        }
    }

    /**
     * Test 8: Verify redeployed dynamic deployment has fresh metrics
     */
    @Test
    @InSequence(8)
    public void verifyRedeployedMetrics() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            PrometheusMetric dynamicMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DYNAMIC_DEPLOYMENT + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Redeployed deployment metrics not found"));

            // Should have fresh counter value (3 from redeploy, not 7+3)
            int value = Integer.parseInt(dynamicMetric.getValue());
            Assert.assertEquals("Redeployed deployment should have fresh metrics", 3, value);
        });
    }

    /**
     * Test 9: Cleanup - undeploy dynamic deployment
     */
    @Test
    @InSequence(9)
    public void cleanup() {
        deployer.undeploy(DYNAMIC_DEPLOYMENT);
    }
}
