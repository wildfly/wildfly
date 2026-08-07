/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.filters;


import static org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask.PROMETHEUS_CONTEXT;

import java.io.IOException;
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
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.observability.PrometheusClient;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@RunWith(Arquillian.class)
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Community.class,
        MicrometerSetupTask.class,
        PrometheusSetupTask.class,
        MicrometerFilterSetupTask.class})
@TestcontainersRequired
@RunAsClient
public class MicrometerFilterTest {
    public static final int REQUEST_COUNT = 5;
    public static final String DEPLOYMENT_NAME = "micrometer-filter-test.war";
    @ContainerResource
    protected ManagementClient managementClient;
    @ArquillianResource
    private URL url;
    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;
    private PrometheusClient prometheusClient;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(JaxRsActivator.class, MicrometerFilterResource.class);
    }

    @Test
    @InSequence(1)
    public void testStartsWithAndEquals() throws URISyntaxException, IOException {
        makeRequests();

        List<PrometheusMetric> metrics = getClient().fetchMetrics();
        List<String> shouldHave = List.of("demo2", "demo5");
        List<String> shouldNotHave = List.of("demo1_total", "demo4", "jvm_", "memory_", "messaging_",
                "transactions_", "undertow_", "gc_", "io_", "buffer_pool_", "classloader_");

        assertMetricsPresent(metrics, shouldHave);
        assertMetricsAbsent(metrics, shouldNotHave);
    }

    @Test
    @InSequence(2)
    public void testEndsWithAndContains() throws URISyntaxException, IOException {
        makeRequests();

        List<PrometheusMetric> metrics = getClient().fetchMetrics();
        // demo3 accepted by ends-with "3", demo2-1 (prometheus: demo2_1) accepted by contains "2-"
        List<String> shouldHave = List.of("demo3", "demo2_1");

        assertMetricsPresent(metrics, shouldHave);
    }

    @Test
    @InSequence(3)
    public void testTagFilters() throws URISyntaxException, IOException {
        makeRequests();

        List<PrometheusMetric> metrics = getClient().fetchMetrics();
        // tagged.alpha (env=prod) accepted by meter-name equals
        List<String> shouldHave = List.of("tagged_alpha");
        // tagged.bravo rejected by tag-value "staging"
        // tagged.charlie and tagged.delta rejected by tag-name "priority"
        List<String> shouldNotHave = List.of("tagged_bravo", "tagged_charlie", "tagged_delta");

        assertMetricsPresent(metrics, shouldHave);
        assertMetricsAbsent(metrics, shouldNotHave);
    }

    @Test
    @InSequence(4)
    public void testNegateFilter() throws URISyntaxException, IOException {
        makeRequests();

        List<PrometheusMetric> metrics = getClient().fetchMetrics();
        // negtest.keep (env=prod) survives: negate filter checks tag-value "prod", finds match, negates to false
        List<String> shouldHave = List.of("negtest_keep");
        // negtest.drop (env=dev) rejected: no tag value matches "prod", negated to true -> DENY
        List<String> shouldNotHave = List.of("negtest_drop");

        assertMetricsPresent(metrics, shouldHave);
        assertMetricsAbsent(metrics, shouldNotHave);
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

    private PrometheusClient getClient() {
        if (this.prometheusClient == null) {
            prometheusClient = new PrometheusClient(String.format("http://%s:%d/%s",
                    managementClient.getMgmtAddress(), managementClient.getMgmtPort(), PROMETHEUS_CONTEXT));
        }

        return prometheusClient;
    }

}
