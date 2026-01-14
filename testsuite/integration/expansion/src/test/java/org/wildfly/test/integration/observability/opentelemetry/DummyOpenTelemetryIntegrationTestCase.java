/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.collector.InMemoryCollector;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerResponse;
import org.jboss.as.test.shared.observability.signals.trace.Span;
import org.jboss.as.test.shared.observability.signals.trace.Trace;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService1;

@RunWith(Arquillian.class)
@ServerSetup({OpenTelemetrySetupTask.class})
@RunAsClient
public class DummyOpenTelemetryIntegrationTestCase {
    private static final String DEPLOYMENT_NAME = "otelinteg";
    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
            // Lower the interval from 60 seconds to 2 seconds
            "otel.metric.export.interval=2000";
    private static final int port = 4317;
    public static InMemoryCollector server;

    static WebArchive buildBaseArchive(String name) {
        return ShrinkWrap
                .create(WebArchive.class, name + ".war")
                .addClasses(
                        BaseOpenTelemetryTest.class,
                        JaxRsActivator.class,
                        OtelService1.class,
                        OtelMetricResource.class
                )
                .addPackage(JaegerResponse.class.getPackage())
                .addAsManifestResource(new StringAsset(MP_CONFIG), "microprofile-config.properties")
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml")
                ;
    }

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    @BeforeClass
    public static void setup() throws IOException {
        server = new InMemoryCollector(port);
        server.start();
    }

    @AfterClass
    public static void stopServer() throws InterruptedException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testTracesReceived() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(getDeploymentUrl(DEPLOYMENT_NAME)).request().get();
            Assert.assertEquals(200, response.getStatus());
        }

        server.assertTraces(traces -> {
                    Assert.assertFalse("Traces not found for service", traces.isEmpty());

                    Trace trace = traces.get(0);
                    String traceId = trace.traceId();
                    List<Span> spans = trace.spans();

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

    public static class OpenTelemetryWithDummyCollectorSetupTask extends OpenTelemetrySetupTask {

        private static final int port = 4317;
        public static InMemoryCollector inMemoryCollector;
        public static DummyCollectorServer server;

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            super.setup(managementClient, containerId);
//            inMemoryCollector = new InMemoryCollector();
//            inMemoryCollector.start(port);

            server = new DummyCollectorServer();
            new Thread(() -> {
                try {
                    server.start(port);
                    server.blockUntilShutdown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();

//            executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "endpoint",
//                    "http://localhost:" + port));

//            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            super.tearDown(managementClient, containerId);

            ServerReload.executeReloadAndWaitForCompletion(managementClient);

            server.stop();

            // Stop the container last to avoid spurious connection errors from the GrpcExporter
            if (inMemoryCollector != null) {
                inMemoryCollector.shutdown();
            }
        }
    }
}
