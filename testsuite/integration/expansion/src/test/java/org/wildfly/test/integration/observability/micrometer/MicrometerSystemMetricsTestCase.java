/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask.MICROMETER_SUBSYSTEM;
import static org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask.PROMETHEUS_CONTEXT;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.apache.http.client.HttpResponseException;
import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.PrometheusClient;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ExtendWith(ArquillianExtension.class)
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class, PrometheusSetupTask.class})
@TestcontainersRequired
@RunAsClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MicrometerSystemMetricsTestCase {
    private static final int REQUEST_COUNT = 5;
    private final List<String> systemMetrics = Arrays.asList(
            "classloader_loaded_classes_count",
            "cpu_available_processors",
            "cpu_system_load_average",
            "gc_time",
            "jvm_classes_loaded",
            "memory_commited_heap_bytes",
            "memory_used_heap",
            "system_cpu_count",
            "thread_count"
    );
    @ContainerResource
    protected ManagementClient managementClient;
    @Testcontainer
    protected OpenTelemetryCollectorContainer otelCollector;
    @ArquillianResource
    private URL url;
    private PrometheusClient prometheusClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-system-metrics.war")
                .addClasses(JaxRsActivator.class, MicrometerResource.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    @Order(0)
    public void basicSmokeTest() throws Exception {
        makeRequests();

        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> results = otelCollector.getMetricsByName(prometheusMetrics, "demo_counter_total"); // Adjust for Prometheus naming conventions

            Assertions.assertEquals(1, results.size());
            results.forEach(r -> Assertions.assertEquals("" + REQUEST_COUNT, r.getValue()));

            Assertions.assertNotEquals(0, otelCollector.getMetricsByName(prometheusMetrics, "demo_timer_milliseconds_count").size());
        });
    }

    @Test
    @Order(1)
    public void testSystemMetricsDisabled() throws Exception {
        setSystemMetrics(false);

        makeRequests();
        List<PrometheusMetric> prometheusMetrics = fetchPrometheusMetrics();
        systemMetrics.forEach(n -> Assertions.assertTrue(
                prometheusMetrics.stream().noneMatch(m -> m.getKey().startsWith(n)),
                "System metrics should not be registered: " + n));
    }

    @Test
    @Order(1)
    public void testSystemMetricsReenabled() throws Exception {
        setSystemMetrics(true);

        makeRequests();
        List<PrometheusMetric> prometheusMetrics = fetchPrometheusMetrics();
        systemMetrics.forEach(n -> Assertions.assertTrue(
                prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(n)),
                "System metrics should be registered: " + n));
    }

    private void setSystemMetrics(boolean enabled) throws Exception {
        executeOp(managementClient,
                Operations.createWriteAttributeOperation(MICROMETER_SUBSYSTEM, "system-metrics",
                        enabled));
        ServerReload.reloadIfRequired(managementClient);
    }

    private void executeOp(final ManagementClient client, final ModelNode op) throws IOException {
        final ModelNode result = client.getControllerClient().execute(Operation.Factory.create(op));
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to execute operation: " + Operations.getFailureDescription(result)
                    .asString());
        }
    }

    private void makeRequests() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < REQUEST_COUNT; i++) {
                target.request().get();
            }
        }
    }

    private List<PrometheusMetric> fetchPrometheusMetrics() throws HttpResponseException {
        if (this.prometheusClient == null) {
            String url = String.format("http://%s:%d/%s", managementClient.getMgmtAddress(), managementClient.getMgmtPort(),
                    PROMETHEUS_CONTEXT);
            prometheusClient = new PrometheusClient(url, "testSuite", "testSuitePassword");
        }

        return prometheusClient.fetchMetrics(false);
    }
}
