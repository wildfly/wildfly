/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask.OPENTELEMETRY_ADDRESS;

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
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class, OpenTelemetryWithCollectorSetupTask.class})
@RunAsClient
public class OpenTelemetrySystemMetricsTestCase extends BaseOpenTelemetryTest {
    private static final int REQUEST_COUNT = 5;
    private static final String DEPLOYMENT_NAME = "otel-system-metrics-test";
    private final String SYSTEM_METRIC_PREFIX = "jvm";

    @ContainerResource
    protected ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    //    @Test
//    @InSequence(1)
    public void basicSmokeTest() throws Exception {
        makeRequests();

        otelCollector.assertMetrics(prometheusMetrics -> {
            List<String> metricsToTest = List.of(OtelMetricResource.COUNTER_NAME);
            metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                    prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(n))));
        });
    }

    @Test
    @InSequence(2)
    public void testSystemMetricsDisabled() throws Exception {
        setSystemMetrics(false);

        makeRequests();

        otelCollector.assertMetrics(prometheusMetrics ->
                Assert.assertTrue("System metrics should not be registered",
                        prometheusMetrics.stream().noneMatch(m -> m.getKey().startsWith(SYSTEM_METRIC_PREFIX))));

    }

    @Test
    @InSequence(3)
    public void testSystemMetricsReenabled() throws Exception {
        setSystemMetrics(true);

        makeRequests();

        otelCollector.assertMetrics(prometheusMetrics ->
                Assert.assertTrue("System metrics should be registered",
                        prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(SYSTEM_METRIC_PREFIX))));
    }

    private void setSystemMetrics(boolean enabled) throws Exception {
        executeOp(managementClient,
                Operations.createWriteAttributeOperation(OPENTELEMETRY_ADDRESS, "system-metrics", enabled));
        resetCollector();
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    private void resetCollector() throws Exception {
        otelCollector.stop();
        otelCollector.start();
        executeOp(managementClient, Operations.createWriteAttributeOperation(OPENTELEMETRY_ADDRESS, "endpoint",
                otelCollector.getOtlpGrpcEndpoint()));
    }

    private void executeOp(final ManagementClient client, final ModelNode op) throws Exception {
        final ModelNode result = client.getControllerClient().execute(Operation.Factory.create(op));
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to execute operation: " + Operations.getFailureDescription(result)
                    .asString());
        }
    }

    private void makeRequests() throws MalformedURLException {
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
}
