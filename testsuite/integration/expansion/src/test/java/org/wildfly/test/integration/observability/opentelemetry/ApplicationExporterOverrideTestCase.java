/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

/**
 * Verifies that application-local exporter selectors own only their selected signal pipeline.
 */
@RunAsClient
@ServerSetup(ApplicationExporterOverrideTestCase.GlobalLogsExporterSetupTask.class)
public class ApplicationExporterOverrideTestCase extends BaseOpenTelemetryTest {
    private static final String SERVER_OWNED = "server-owned";
    private static final String APPLICATION_OWNED = "application-owned";
    private static final String APPLICATION_METRICS = "application-metrics";
    private static final String APPLICATION_TRACES_NONE = "application-traces-none";
    private static final String INVALID_EXPORTER = "invalid-exporter";
    private static final String OBSOLETE_MODULE = "obsolete-telemetry-module";
    private static final String EXPECTED_LOG_ENTRY = "This is a test message: application-exporter";

    @ArquillianResource
    private Deployer deployer;

    /** Creates the deployment that inherits the server-owned logs selector. */
    @Deployment(name = SERVER_OWNED, order = 1, testable = false)
    public static WebArchive serverOwnedDeployment() {
        return buildBaseArchive(SERVER_OWNED);
    }

    /** Creates the deployment with an application-owned logs exporter. */
    @Deployment(name = APPLICATION_OWNED, order = 2, testable = false)
    public static WebArchive applicationOwnedDeployment() {
        return applicationDeployment(APPLICATION_OWNED, "otel.logs.exporter", "otlp");
    }

    /** Creates the deployment with an application-owned metrics exporter. */
    @Deployment(name = APPLICATION_METRICS, order = 3, testable = false)
    public static WebArchive applicationMetricsDeployment() {
        return applicationDeployment(APPLICATION_METRICS, "otel.metrics.exporter", "otlp");
    }

    /** Creates the deployment that explicitly discards application-owned traces. */
    @Deployment(name = APPLICATION_TRACES_NONE, order = 4, testable = false)
    public static WebArchive applicationTracesNoneDeployment() {
        return applicationDeployment(APPLICATION_TRACES_NONE, "otel.traces.exporter", "none");
    }

    /** Creates the unmanaged deployment whose invalid exporter must reject deployment. */
    @Deployment(name = INVALID_EXPORTER, managed = false, testable = false)
    public static WebArchive invalidExporterDeployment() {
        return applicationDeployment(INVALID_EXPORTER, "otel.logs.exporter", "missing-exporter");
    }

    /** Creates an unmanaged deployment that requires the obsolete private telemetry API module. */
    @Deployment(name = OBSOLETE_MODULE, managed = false, testable = false)
    public static WebArchive obsoleteModuleDeployment() {
        return buildBaseArchive(OBSOLETE_MODULE)
                .addAsManifestResource(new StringAsset(
                        "Dependencies: org.wildfly.extension.microprofile.telemetry-api\n"), "MANIFEST.MF");
    }

    /**
     * Verifies signal-specific application ownership while unselected signals remain server-owned.
     *
     * @throws Exception if a request or collector assertion fails
     */
    @Test
    @InSequence(1)
    public void applicationSelectorsOwnOnlyTheirSignals() throws Exception {
        makeRequests(new URL(getDeploymentUrl(SERVER_OWNED) + "logging/application-exporter"), 1,
                Response.Status.NO_CONTENT.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(APPLICATION_OWNED) + "logging/application-exporter"), 1,
                Response.Status.NO_CONTENT.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(APPLICATION_OWNED) + "?name=application-owned"), 1,
                Response.Status.OK.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(APPLICATION_OWNED) + "metrics"), 1,
                Response.Status.OK.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(APPLICATION_METRICS) + "?name=application-metrics"), 1,
                Response.Status.OK.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(APPLICATION_METRICS) + "metrics"), 1,
                Response.Status.OK.getStatusCode());
        makeRequests(new URL(getDeploymentUrl(APPLICATION_TRACES_NONE) + "?name=application-traces-none"), 1,
                Response.Status.OK.getStatusCode());

        otelCollector.assertOpenTelemetryLogs(logs -> {
            Assert.assertTrue("Application-owned log was not exported",
                    logs.stream().anyMatch(log ->
                            log.body().contains(EXPECTED_LOG_ENTRY)
                                    && (APPLICATION_OWNED + ".war").equals(
                                            log.resourceAttributes().get("service.name"))));
            Assert.assertFalse("Server-owned logs exporter ignored the global none selector",
                    logs.stream().anyMatch(log ->
                            log.body().contains(EXPECTED_LOG_ENTRY)
                                    && (SERVER_OWNED + ".war").equals(
                                            log.resourceAttributes().get("service.name"))));
        });

        otelCollector.assertTraces(APPLICATION_OWNED + ".war", traces ->
                Assert.assertFalse("Application traces were discarded", traces.isEmpty()));
        otelCollector.assertTraces(APPLICATION_METRICS + ".war", traces ->
                Assert.assertFalse("Unselected application traces were discarded", traces.isEmpty()));
        Assert.assertTrue("Application-selected none traces fell back to the server exporter",
                otelCollector.getTraces(APPLICATION_TRACES_NONE + ".war").isEmpty());
        otelCollector.assertMetrics(metrics -> {
            List<PrometheusMetric> applicationMetrics = otelCollector.getMetricsByName(metrics,
                    OtelMetricResource.COUNTER_NAME + "_total");
            Assert.assertTrue("Application-owned metrics were discarded",
                    applicationMetrics.stream().anyMatch(metric ->
                            Objects.equals(APPLICATION_METRICS + ".war", metric.getTags().get("job"))));
            Assert.assertFalse("Unselected metrics ignored the server-owned none selector",
                    applicationMetrics.stream().anyMatch(metric ->
                            Objects.equals(APPLICATION_OWNED + ".war", metric.getTags().get("job"))));
        });
    }

    /** Verifies that an invalid application exporter prevents deployment. */
    @Test
    @InSequence(2)
    public void invalidApplicationExporterFailsDeployment() {
        try {
            deployer.deploy(INVALID_EXPORTER);
            Assert.fail("Invalid application exporter configuration should fail deployment");
        } catch (Exception expected) {
            Assert.assertTrue("Missing exporter name from deployment failure",
                    causeContains(expected, "missing-exporter"));
        }
    }

    /** Verifies that the obsolete private telemetry API module is no longer available to deployments. */
    @Test
    @InSequence(3)
    public void obsoleteTelemetryApiModuleIsUnavailable() {
        try {
            deployer.deploy(OBSOLETE_MODULE);
            Assert.fail("The obsolete telemetry API module should not be available");
        } catch (Exception expected) {
            Assert.assertTrue("Deployment did not fail because the obsolete module is missing",
                    causeContains(expected, "org.wildfly.extension.microprofile.telemetry-api"));
        }
    }

    /**
     * Tests whether any exception in a cause chain identifies the expected deployment failure.
     *
     * @param failure the deployment failure
     * @param expectedText text that must occur in the failure chain
     * @return {@code true} when the expected text occurs
     */
    private static boolean causeContains(Throwable failure, String expectedText) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.toString().contains(expectedText)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a deployment whose packaged selector outranks the server system property.
     *
     * @param name the deployment name
     * @param exporterProperty the application exporter selector property
     * @param exporter the application exporter selector value
     * @return the configured deployment archive
     */
    private static WebArchive applicationDeployment(String name, String exporterProperty, String exporter) {
        WebArchive archive = buildBaseArchive(name);
        archive.delete("/META-INF/microprofile-config.properties");
        return archive.addAsManifestResource(new StringAsset(
                "config_ordinal=500\n"
                        + "otel.sdk.disabled=false\n"
                        + "otel.metric.export.interval=2000\n"
                        + exporterProperty + "=" + exporter),
                "microprofile-config.properties");
    }

    /** Configures the collector and disables the server-owned logs and metrics exporters globally. */
    public static final class GlobalLogsExporterSetupTask extends OpenTelemetryWithCollectorSetupTask {
        private static final ModelNode LOGS_EXPORTER_ADDRESS =
                Operations.createAddress("system-property", "otel.logs.exporter");
        private static final ModelNode METRICS_EXPORTER_ADDRESS =
                Operations.createAddress("system-property", "otel.metrics.exporter");

        /** {@inheritDoc} */
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);
            ModelNode add = Operations.createAddOperation(LOGS_EXPORTER_ADDRESS);
            add.get(ModelDescriptionConstants.VALUE).set("none");
            executeOp(managementClient, add);
            add = Operations.createAddOperation(METRICS_EXPORTER_ADDRESS);
            add.get(ModelDescriptionConstants.VALUE).set("none");
            executeOp(managementClient, add);
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        /** {@inheritDoc} */
        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            executeOp(managementClient, Operations.createRemoveOperation(LOGS_EXPORTER_ADDRESS));
            executeOp(managementClient, Operations.createRemoveOperation(METRICS_EXPORTER_ADDRESS));
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
            super.tearDown(managementClient, containerId);
        }
    }
}
