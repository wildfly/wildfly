/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry.filters;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ExtendWith(ArquillianExtension.class)
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class, OpenTelemetryFilterSetupTask.class})
@TestcontainersRequired
@RunAsClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenTelemetryFilterTest {
    public static final int REQUEST_COUNT = 5;
    public static final String DEPLOYMENT_NAME = "otel-filter-test.war";

    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
            "otel.metric.export.interval=100";

    @ArquillianResource
    private URL url;
    @ContainerResource
    private ManagementClient managementClient;
    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(JaxRsActivator.class, OpenTelemetryFilterResource.class)
                .addAsManifestResource(new StringAsset(MP_CONFIG), "microprofile-config.properties")
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    @Order(1)
    public void testStartsWithAndEquals() throws URISyntaxException, InterruptedException {
        makeRequests();

        List<String> shouldHave = List.of("demo2", "demo5");
        List<String> shouldNotHave = List.of("demo1_total", "demo4", "jvm_", "memory_", "messaging_",
                "transactions_", "undertow_", "gc_", "io_", "buffer_pool_", "classloader_");

        otelCollector.assertMetrics(metrics -> {
            assertMetricsPresent(metrics, shouldHave);
            assertMetricsAbsent(metrics, shouldNotHave);
        });
    }

    @Test
    @Order(2)
    public void testEndsWithAndContains() throws URISyntaxException, InterruptedException {
        makeRequests();

        // demo3 accepted by ends-with "3", demo2-1 (prometheus: demo2_1) accepted by contains "2-"
        List<String> shouldHave = List.of("demo3", "demo2_1");

        otelCollector.assertMetrics(metrics -> assertMetricsPresent(metrics, shouldHave));
    }

    @Test
    @Order(3)
    public void testTagFilters() throws URISyntaxException, InterruptedException {
        makeRequests();

        // ACCEPT: meter-name equals, tag-value starts-with/ends-with/contains, tag-name starts-with.
        List<String> shouldHave = List.of("tagged_alpha", "tagged_echo", "tagged_foxtrot", "tagged_golf", "tagged_hotel");
        // REJECT: tag-value equals/starts-with/ends-with/contains and tag-name equals/starts-with/ends-with/contains.
        List<String> shouldNotHave = List.of("tagged_bravo", "tagged_charlie", "tagged_delta", "tagged_india",
                "tagged_juliet", "tagged_kilo", "tagged_lima", "tagged_mike", "tagged_november");

        otelCollector.assertMetrics(metrics -> {
            assertMetricsPresent(metrics, shouldHave);
            assertMetricsAbsent(metrics, shouldNotHave);
        });
    }

    @Test
    @Order(4)
    public void testNegateFilter() throws URISyntaxException, InterruptedException {
        makeRequests();

        // negtest.keep (env=prod) survives: negate filter checks tag-value "prod", finds match, negates to false
        List<String> shouldHave = List.of("negtest_keep");
        // negtest.drop (env=dev) rejected: no tag value matches "prod", negated to true -> DENY
        List<String> shouldNotHave = List.of("negtest_drop");

        otelCollector.assertMetrics(metrics -> {
            assertMetricsPresent(metrics, shouldHave);
            assertMetricsAbsent(metrics, shouldNotHave);
        });
    }

    /**
     * Verifies that every generic meter type is accepted only when OpenTelemetry can translate it.
     */
    @Test
    @Order(5)
    public void testMeterTypeFilter() throws Exception {
        Map<String, String> supportedTypes = Map.of(
                "sum", "type_sum",
                "gauge", "type_gauge",
                "histogram", "type_histogram");

        try (CLIWrapper cli = new CLIWrapper(true)) {
            for (String type : List.of("sum", "gauge", "histogram")) {
                String filter = "type-test-" + type;
                cli.sendLine("/subsystem=opentelemetry/filter=" + filter
                        + ":add(outcome=reject, field=meter-type, condition=equals, value=" + type + ")");
                ServerReload.executeReloadAndWaitForCompletion(managementClient);
                try {
                    makeRequests();
                    otelCollector.assertMetrics(metrics -> assertMetricsAbsent(metrics, List.of(supportedTypes.get(type))));
                } finally {
                    cli.sendLine("/subsystem=opentelemetry/filter=" + filter + ":remove");
                    ServerReload.executeReloadAndWaitForCompletion(managementClient);
                }
            }

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

    private void assertMetricsPresent(List<PrometheusMetric> metrics, List<String> shouldHave) {
        shouldHave.forEach(metric ->
                Assertions.assertTrue(metrics.stream().anyMatch(m -> m.getKey().startsWith(metric)),
                        "The metric '" + metric + "' was not found in the metrics list"));
    }

    private void assertMetricsAbsent(List<PrometheusMetric> metrics, List<String> shouldNotHave) {
        shouldNotHave.forEach(metric ->
                Assertions.assertTrue(metrics.stream().noneMatch(m -> m.getKey().startsWith(metric)),
                        "The metric '" + metric + "' should not be found in the metrics list"));
    }

}
