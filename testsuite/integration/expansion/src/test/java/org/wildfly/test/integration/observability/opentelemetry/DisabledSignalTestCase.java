/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.wildfly.test.integration.observability.opentelemetry.OpenTelemetryManagementUtils.reload;
import static org.wildfly.test.integration.observability.opentelemetry.OpenTelemetryManagementUtils.setOtlpEndpoint;
import static org.wildfly.test.integration.observability.opentelemetry.OpenTelemetryManagementUtils.setSignalEnabledStatus;
import static org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource.TEST_MESSAGE;

import java.net.MalformedURLException;
import java.net.URLPermission;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.PropertyPermission;
import java.util.logging.Logger;

import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerSpan;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;
import org.wildfly.test.integration.observability.opentelemetry.exporter.JsonLogRecordExporter;
import org.wildfly.test.integration.observability.opentelemetry.exporter.JsonLogRecordExporterProvider;
import org.wildfly.test.integration.observability.setuptask.LogExporterSetupTask;
import org.wildfly.test.integration.observability.setuptask.TestLogRecordReceiver;

@ServerSetup({OpenTelemetryWithCollectorSetupTask.class, LogExporterSetupTask.class})
@DockerRequired
@RunAsClient
public class DisabledSignalTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_NAME1 = "enabled-signals";
    private static final int REQUEST_COUNT = 5;
    //    private final OtlpLogReceiver otlpLogReceiver = OtlpLogReceiver.INSTANCE;
    private static final TestLogRecordReceiver receiver = new TestLogRecordReceiver(1223);

    @ContainerResource
    ManagementClient managementClient;

    @Deployment(testable = false)
    public static WebArchive getDeployment1() {
        return buildBaseArchive(DEPLOYMENT_NAME1)
            .addClasses(
                JsonLogRecordExporter.class,
                JsonLogRecordExporterProvider.class
            )
            .addAsServiceProvider(ConfigurableLogRecordExporterProvider.class, JsonLogRecordExporterProvider.class)
            .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                createPermissions("wildfly.opentelemetry.logs.json.exporter")), "permissions.xml");
    }

    @AfterClass
    public static void afterClass() {
        receiver.stop();
    }

    @Test
    @InSequence(1)
    public void makeRequests() throws MalformedURLException {
        final String testName = "TeamCity";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(getDeploymentUrl(DEPLOYMENT_NAME1) + "/metrics?name=" + testName);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Response response = target.request().get();
                Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                Assert.assertEquals("Hello, " + testName, response.readEntity(String.class));
            }
        }
    }

    @Test
    @InSequence(2)
    public void metricsFound() throws InterruptedException {
        List<String> metricsToTest = List.of(OtelMetricResource.COUNTER_NAME);

        otelCollector.assertMetrics(prometheusMetrics -> metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
            prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(n)))));
    }

    @Test
    @InSequence(3)
    public void tracesFound() throws InterruptedException {
        otelCollector.assertTraces(DEPLOYMENT_NAME1 + ".war", traces -> {
            Assert.assertFalse("Traces not found for service", traces.isEmpty());

            JaegerTrace trace = traces.get(0);
            String traceId = trace.getTraceID();
            List<JaegerSpan> spans = trace.getSpans();

            spans.forEach(s ->
                Assert.assertEquals("The traceId of the span did not match the first span's. Context propagation failed.",
                    traceId, s.getTraceID()));
        });
    }

    @Test
    @InSequence(4)
    public void logsFound() throws InterruptedException {
        Logger.getLogger(getClass().getSimpleName()).warning(TEST_MESSAGE);

        receiver.assertLogs(logs -> {
            Assert.assertTrue(logs.stream().anyMatch(lr ->
                (lr.getBodyValue() != null) && lr.getBodyValue().asString().contains(TEST_MESSAGE)));
        });
    }

    @Test
    @InSequence(5)
    public void disableSignals() {
        otelCollector.stop();
        otelCollector.start();

        // Set subsystem attributes to disable the signals
        setSignalEnabledStatus(managementClient.getControllerClient(), "metrics", false);
        setSignalEnabledStatus(managementClient.getControllerClient(), "traces", false);
        setSignalEnabledStatus(managementClient.getControllerClient(), "logs", false);
        setOtlpEndpoint(managementClient.getControllerClient(), otelCollector.getOtlpGrpcEndpoint());

        reload(managementClient.getControllerClient());
        receiver.reset();
    }

    @Test
    @InSequence(6)
    public void makeMoreRequests() throws MalformedURLException {
        makeRequests();
    }

    @Test
    @InSequence(7)
    public void metricsNotFound() throws InterruptedException {
        List<String> metricsToTest = List.of(OtelMetricResource.COUNTER_NAME);

        otelCollector.assertMetrics(prometheusMetrics ->
            metricsToTest.forEach(n -> Assert.assertTrue("Unwanted metric found: " + n,
                prometheusMetrics.stream().noneMatch(m -> m.getKey().startsWith(n)))));
    }

    @Test
    @InSequence(8)
    public void tracesNotFound() throws InterruptedException {
        otelCollector.assertTraces(DEPLOYMENT_NAME1 + ".war", traces ->
            Assert.assertTrue("Traces found for service", traces.isEmpty()));
    }

    @Test
    @InSequence(9)
    public void logsNotFound() throws InterruptedException {
        Logger.getLogger(getClass().getSimpleName()).warning(TEST_MESSAGE);

        receiver.assertLogs(logs -> Assert.assertTrue(logs.isEmpty()));
    }


    @Test
    @InSequence(10)
    public void enableSignals() {
        // Set subsystem attributes to re-ensable the signals
        setSignalEnabledStatus(managementClient.getControllerClient(), "metrics", true);
        setSignalEnabledStatus(managementClient.getControllerClient(), "traces", true);
        setSignalEnabledStatus(managementClient.getControllerClient(), "logs", true);

        reload(managementClient.getControllerClient());
    }

    protected static Permission[] createPermissions(final String... names) {
        final Collection<Permission> permissions = new ArrayList<>(names.length * 2);
        for (String name : names) {
            permissions.add(new URLPermission("http://localhost:1223"));
            permissions.add(new PropertyPermission(name, "read"));
            permissions.add(new RuntimePermission("getenv." + name));
            permissions.add(new RuntimePermission("getenv." + name.replace('.', '_')));
            permissions.add(new RuntimePermission("getenv." + name.toUpperCase(Locale.ROOT)));
            permissions.add(new RuntimePermission("getenv." + name.replace('.', '_').toUpperCase(Locale.ROOT)));
        }
        return permissions.toArray(new Permission[0]);
    }
}
