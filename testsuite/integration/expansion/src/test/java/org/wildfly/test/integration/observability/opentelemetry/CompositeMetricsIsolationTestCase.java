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
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

/**
 * Verifies that metrics from multiple deployments retain distinct resource identities in exported data.
 */
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
@TestcontainersRequired
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CompositeMetricsIsolationTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_ONE = "metrics-app-one";
    private static final String DEPLOYMENT_TWO = "metrics-app-two";
    private static final String DEPLOYMENT_THREE = "metrics-app-three";

    private static final int REQUEST_COUNT_ONE = 5;
    private static final int REQUEST_COUNT_TWO = 10;
    private static final int REQUEST_COUNT_THREE = 3;

    @ArquillianResource
    private Deployer deployer;

    /** Creates the first managed metrics deployment. */
    @Deployment(name = DEPLOYMENT_ONE, order = 1, testable = false)
    public static WebArchive createDeployment1() {
        return buildBaseArchive(DEPLOYMENT_ONE);
    }

    /** Creates the second managed metrics deployment. */
    @Deployment(name = DEPLOYMENT_TWO, order = 2, testable = false)
    public static WebArchive createDeployment2() {
        return buildBaseArchive(DEPLOYMENT_TWO);
    }

    /** Creates the unmanaged deployment used to verify dynamic registration. */
    @Deployment(name = DEPLOYMENT_THREE, order = 3, testable = false, managed = false)
    public static WebArchive createDeployment3() {
        return buildBaseArchive(DEPLOYMENT_THREE);
    }

    /** Records metrics for the first deployment. */
    @Test
    @Order(1)
    @OperateOnDeployment(DEPLOYMENT_ONE)
    public void makeRequestsToDeploymentOne() throws Exception {
        makeRequests(new URL(getDeploymentUrl(DEPLOYMENT_ONE) + "metrics?name=" + DEPLOYMENT_ONE),
                REQUEST_COUNT_ONE, Response.Status.OK.getStatusCode());
    }

    /** Records metrics for the second deployment. */
    @Test
    @Order(2)
    @OperateOnDeployment(DEPLOYMENT_TWO)
    public void makeRequestsToDeploymentTwo() throws Exception {
        makeRequests(new URL(getDeploymentUrl(DEPLOYMENT_TWO) + "metrics?name=" + DEPLOYMENT_TWO),
                REQUEST_COUNT_TWO, Response.Status.OK.getStatusCode());
    }

    /** Verifies both deployments export counters under distinct resource identities. */
    @Test
    @Order(3)
    public void verifyMetricIsolation() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            Assertions.assertFalse(metrics.isEmpty(), "No metrics found");

            // Verify deployment 1 metrics
            PrometheusMetric deploymentOneMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_ONE + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment one metric not found"));

            PrometheusMetric deploymentTwoMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_TWO + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment two metric not found"));

            Assertions.assertTrue(Integer.parseInt(deploymentOneMetric.getValue()) > 0,
                                  "Deployment one counter should contain data");
            Assertions.assertTrue(Integer.parseInt(deploymentTwoMetric.getValue()) > 0,
                                  "Deployment two counter should contain data");
        });
    }

    /** Verifies deployment service names are exported as resource-derived job tags. */
    @Test
    @Order(4)
    public void verifyResourceAttributes() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            // Check deployment one has correct job tag (service.name)
            boolean foundDeploymentOne = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_ONE + ".war"));
            Assertions.assertTrue(foundDeploymentOne,
                                  "Deployment one should have job=" + DEPLOYMENT_ONE + ".war");

            // Check deployment two has correct job tag (service.name)
            boolean foundDeploymentTwo = metrics.stream()
                .anyMatch(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_TWO + ".war"));
            Assertions.assertTrue(foundDeploymentTwo,
                                  "Deployment two should have job=" + DEPLOYMENT_TWO + ".war");
        });
    }

    /** Deploys a third application dynamically and records metrics for it. */
    @Test
    @Order(5)
    public void deployThirdDeployment() throws Exception {
        // DEPLOYMENT_THREE is declared managed = false, so it must be deployed explicitly.
        deployer.deploy(DEPLOYMENT_THREE);
        makeRequests(new URL(getDeploymentUrl(DEPLOYMENT_THREE) + "metrics?name=" + DEPLOYMENT_THREE),
                REQUEST_COUNT_THREE, Response.Status.OK.getStatusCode());
    }

    /** Verifies a periodic export contains metrics from all three registered deployments. */
    @Test
    @Order(6)
    public void verifyThirdDeploymentMetrics() throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> metrics = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total");

            // Should now have metrics from all three deployments
            long deploymentCount = metrics.stream()
                .map(m -> m.getTags().get("job"))
                .distinct()
                .filter(job -> job != null && job.endsWith(".war"))
                .count();

            Assertions.assertTrue(deploymentCount >= 3, "Should have at least 3 deployments");

            PrometheusMetric deploymentThreeMetric = metrics.stream()
                .filter(m -> Objects.equals(m.getTags().get("job"), DEPLOYMENT_THREE + ".war"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Deployment three metric not found"));

            Assertions.assertTrue(Integer.parseInt(deploymentThreeMetric.getValue()) > 0,
                                  "Deployment three counter should contain data");
        });
    }

    /** Undeploys the dynamically managed test application. */
    @Test
    @Order(7)
    public void cleanup() {
        deployer.undeploy(DEPLOYMENT_THREE);
    }
}
