/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.as.test.shared.observability.signals.logs.OpenTelemetryLogRecord;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

/**
 * Verifies metrics and logs continue to use the correct deployment identity across undeploy and redeploy.
 */
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
@TestcontainersRequired
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeploymentLifecycleTestCase extends BaseOpenTelemetryTest {
    private static final String PERSISTENT_DEPLOYMENT = "lifecycle-persistent";
    private static final String DYNAMIC_DEPLOYMENT = "lifecycle-dynamic";
    private static final String EXPECTED_LOG_ENTRY = "This is a test message: hello";

    @ArquillianResource
    private Deployer deployer;

    /** Creates the deployment that remains active throughout the test. */
    @Deployment(name = PERSISTENT_DEPLOYMENT, order = 1, testable = false)
    public static WebArchive createPersistentDeployment() {
        return buildBaseArchive(PERSISTENT_DEPLOYMENT);
    }

    /** Creates the unmanaged deployment exercised across lifecycle transitions. */
    @Deployment(name = DYNAMIC_DEPLOYMENT, order = 2, testable = false, managed = false)
    public static WebArchive createDynamicDeployment() {
        return buildBaseArchive(DYNAMIC_DEPLOYMENT);
    }

    /** Records baseline metrics for the persistent deployment. */
    @Test
    @Order(1)
    public void generatePersistentDeploymentMetrics() throws Exception {
        makeRequests(new URL(getDeploymentUrl(PERSISTENT_DEPLOYMENT) + "metrics?name=test"), 5,
                Response.Status.OK.getStatusCode());
    }

    /** Verifies the persistent deployment exports metrics. */
    @Test
    @Order(2)
    public void verifyPersistentDeploymentMetrics() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            boolean found = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), PERSISTENT_DEPLOYMENT + ".war"));

            Assertions.assertTrue(found, "Persistent deployment metrics should be present");
        });
    }

    /** Deploys the dynamic application and generates metrics and a log record. */
    @Test
    @Order(3)
    public void deployDynamicDeployment() throws Exception {
        deployer.deploy(DYNAMIC_DEPLOYMENT);
        makeRequests(new URL(getDeploymentUrl(DYNAMIC_DEPLOYMENT) + "metrics?name=test"), 7,
                Response.Status.OK.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(DYNAMIC_DEPLOYMENT) + "logging/hello"), 1,
                Response.Status.NO_CONTENT.getStatusCode());
    }

    /** Verifies both active deployments appear in exported metrics. */
    @Test
    @Order(4)
    public void verifyBothDeploymentsPresent() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            long deploymentCount = metrics.stream()
                .map(m -> m.getTags().get("job"))
                .filter(job -> (PERSISTENT_DEPLOYMENT + ".war").equals(job) ||
                               (DYNAMIC_DEPLOYMENT + ".war").equals(job))
                .distinct()
                .count();

            Assertions.assertEquals(2, deploymentCount,
                                    "Should have metrics from both deployments");
        });
    }

    /** Undeploys the dynamic application. */
    @Test
    @Order(5)
    public void undeployDynamicDeployment() {
        deployer.undeploy(DYNAMIC_DEPLOYMENT);
    }

    /** Verifies the remaining deployment continues exporting after the other deployment stops. */
    @Test
    @Order(6)
    public void verifyRemainingDeploymentContinuesExporting() throws Exception {
        makeRequests(new URL(getDeploymentUrl(PERSISTENT_DEPLOYMENT) + "metrics?name=test"), 2,
                Response.Status.OK.getStatusCode());

        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            // Persistent deployment should still be present
            boolean persistentFound = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), PERSISTENT_DEPLOYMENT + ".war"));
            Assertions.assertTrue(persistentFound,
                                  "Persistent deployment metrics should still be present");
        });
    }

    /** Redeploys the dynamic application and generates fresh telemetry. */
    @Test
    @Order(7)
    public void redeployDynamicDeployment() throws Exception {
        deployer.deploy(DYNAMIC_DEPLOYMENT);
        makeRequests(new URL(getDeploymentUrl(DYNAMIC_DEPLOYMENT) + "metrics?name=test"), 3,
                Response.Status.OK.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(DYNAMIC_DEPLOYMENT) + "logging/hello"), 1,
                Response.Status.NO_CONTENT.getStatusCode());
    }

    /** Verifies the redeployed application resumes exporting metrics. */
    @Test
    @Order(8)
    public void verifyRedeployedMetrics() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            PrometheusMetric dynamicMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DYNAMIC_DEPLOYMENT + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Redeployed deployment metrics not found"));

            int value = Integer.parseInt(dynamicMetric.getValue());
            Assertions.assertTrue(value > 0,
                                  "Redeployed deployment should export metrics");
        });
    }

    /** Undeploys the dynamic application and verifies both lifecycle log records. */
    @Test
    @Order(9)
    public void cleanup() throws InterruptedException {
        deployer.undeploy(DYNAMIC_DEPLOYMENT);
        assertDynamicDeploymentLogs();
    }

    /** Verifies redeploy replaces the log route instead of creating duplicate routes. */
    private void assertDynamicDeploymentLogs() throws InterruptedException {
        otelCollector.assertOpenTelemetryLogs(logEntries -> {
            List<OpenTelemetryLogRecord> deploymentLogs = logEntries.stream()
                    .filter(log -> log.body().contains(EXPECTED_LOG_ENTRY))
                    .toList();

            Assertions.assertEquals(2, deploymentLogs.size(),
                                    "Unexpected deployment log count");
            Assertions.assertTrue(deploymentLogs.stream().allMatch(log -> (DYNAMIC_DEPLOYMENT + ".war").equals(
                            log.resourceAttributes().get("service.name"))),
                                  "Deployment log exported with the wrong service.name");
        });
    }
}
