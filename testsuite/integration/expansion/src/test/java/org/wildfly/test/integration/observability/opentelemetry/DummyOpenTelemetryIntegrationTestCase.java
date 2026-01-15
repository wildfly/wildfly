/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.setuptasks.AbstractSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

@RunAsClient
public class DummyOpenTelemetryIntegrationTestCase extends BaseOpenTelemetryTest{
    private static final String DEPLOYMENT_NAME = "otelinteg";

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    @Test
    public void testTracesReceived() throws Exception {
//        try (Client client = ClientBuilder.newClient()) {
//            Response response = client.target(getDeploymentUrl(DEPLOYMENT_NAME)).request().get();
//            Assert.assertEquals(200, response.getStatus());
//        }

        makeRequests(new URL(getDeploymentUrl(DEPLOYMENT_NAME) ), 1, 200);

        server.assertSpans(spans -> {
                    Assert.assertFalse("Traces not found for service", spans.isEmpty());

                    String traceId = spans.get(0).traceId();

                    spans.forEach(s ->
                            Assert.assertEquals("The traceId of the span did not match the first span's. Context propagation failed.",
                                    traceId, s.traceId()));
                });
    }

    @Test
    public void testLogsReceived() throws Exception {
        server.assertLogs(logs ->
                Assert.assertFalse("Logs not found", logs.isEmpty()));
    }

    @Test
    public void testMetricsReceived() throws Exception {
        server.assertMetrics(metrics ->
                Assert.assertFalse("Metrics not found", metrics.isEmpty()));
    }

    protected String getDeploymentUrl(String deploymentName) throws MalformedURLException {
        return TestSuiteEnvironment.getHttpUrl() + "/" + deploymentName + "/";
    }

    static class DummyMicrometerSetupTask extends AbstractSetupTask {
        private static final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
        private static final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");
        private static final ModelNode otlpRegistry = Operations.createAddress(SUBSYSTEM, "micrometer", "registry", "otlp");


        @Override
        public void setup(final ManagementClient managementClient, String containerId) throws Exception {
            executeOp(managementClient, writeAttribute("undertow", STATISTICS_ENABLED, "true"));

            if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerExtension))) {
                executeOp(managementClient, Operations.createAddOperation(micrometerExtension));
            }

            if (!Operations.isSuccessfulOutcome(executeRead(managementClient, micrometerSubsystem))) {
                executeOp(managementClient, Operations.createAddOperation(micrometerSubsystem));
            }

            if (!Operations.isSuccessfulOutcome(executeRead(managementClient, otlpRegistry))) {
                ModelNode addOtlpOp = Operations.createAddOperation(otlpRegistry);
                addOtlpOp.get("endpoint").set("http://localhost:4318/v1/metrics");
                addOtlpOp.get("step").set("1");
                executeOp(managementClient, addOtlpOp);
            } else {
                executeOp(managementClient, writeAttribute(otlpRegistry, "endpoint", "http://localhost:4318/v1/metrics"));
            }

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
            executeOp(managementClient, clearAttribute("undertow", STATISTICS_ENABLED));
            executeOp(managementClient, Operations.createRemoveOperation(micrometerSubsystem));
            executeOp(managementClient, Operations.createRemoveOperation(micrometerExtension));

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }
}
