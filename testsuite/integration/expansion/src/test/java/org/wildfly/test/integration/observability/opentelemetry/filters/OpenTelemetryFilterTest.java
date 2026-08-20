/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry.filters;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@RunWith(Arquillian.class)
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class, OpenTelemetryFilterSetupTask.class})
@TestcontainersRequired
@RunAsClient
public class OpenTelemetryFilterTest {
    public static final int REQUEST_COUNT = 5;
    public static final String DEPLOYMENT_NAME = "otel-filter-test.war";

    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
            "otel.metric.export.interval=100";

    @ArquillianResource
    private URL url;
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
    @InSequence(1)
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
    @InSequence(2)
    public void testEndsWithAndContains() throws URISyntaxException, InterruptedException {
        makeRequests();

        // demo3 accepted by ends-with "3", demo2-1 (prometheus: demo2_1) accepted by contains "2-"
        List<String> shouldHave = List.of("demo3", "demo2_1");

        otelCollector.assertMetrics(metrics -> assertMetricsPresent(metrics, shouldHave));
    }

    @Test
    @InSequence(3)
    public void testTagFilters() throws URISyntaxException, InterruptedException {
        makeRequests();

        // tagged.alpha (env=prod) accepted by meter-name equals
        List<String> shouldHave = List.of("tagged_alpha");
        // tagged.bravo rejected by tag-value "staging"
        // tagged.charlie and tagged.delta rejected by tag-name "priority"
        List<String> shouldNotHave = List.of("tagged_bravo", "tagged_charlie", "tagged_delta");

        otelCollector.assertMetrics(metrics -> {
            assertMetricsPresent(metrics, shouldHave);
            assertMetricsAbsent(metrics, shouldNotHave);
        });
    }

    @Test
    @InSequence(4)
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
                Assert.assertTrue("The metric '" + metric + "' was not found in the metrics list",
                        metrics.stream().anyMatch(m -> m.getKey().startsWith(metric))));
    }

    private void assertMetricsAbsent(List<PrometheusMetric> metrics, List<String> shouldNotHave) {
        shouldNotHave.forEach(metric ->
                Assert.assertTrue("The metric '" + metric + "' should not be found in the metrics list",
                        metrics.stream().noneMatch(m -> m.getKey().startsWith(metric))));
    }

}
