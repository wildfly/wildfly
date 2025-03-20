/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@DockerRequired
@RunAsClient
public class MicrometerOtelIntegrationTestCase {
    public static final int REQUEST_COUNT = 5;
    public static final String DEPLOYMENT_NAME = "micrometer-test.war";

    @ArquillianResource
    private URL url;

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-test.war")
                .addClasses(JaxRsActivator.class, MicrometerResource.class);
    }

    // The @ServerSetup(MicrometerSetupTask.class) requires Docker to be available.
    // Otherwise the org.wildfly.extension.micrometer.registry.NoOpRegistry is installed which will result in 0 counters,
    // and cause the test fail seemingly intermittently on machines with broken Docker setup.
    @BeforeClass
    public static void checkForDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }

    @Test
    @InSequence(1)
    public void makeRequests() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < REQUEST_COUNT; i++) {
                target.request().get();
            }
        }
    }

    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify there existence
    @Test
    @InSequence(4)
    public void getMetrics() throws InterruptedException {
        List<String> metricsToTest = Arrays.asList(
                "demo_counter",
                "demo_timer",
                "memory_used_heap",
                "cpu_available_processors",
                "classloader_loaded_classes_count",
                "cpu_system_load_average",
                "gc_time",
                "thread_count",
                "undertow_bytes_received"
        );

        otelCollector.assertMetrics(prometheusMetrics -> metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                prometheusMetrics.stream().anyMatch(m -> m.getKey().startsWith(n)))));
    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void testApplicationModelMetrics() throws InterruptedException {
        List<String> metricsToTest = List.of(
                "undertow_active_sessions",
                "undertow_expired_sessions_total",
                "undertow_highest_session_count",
                "undertow_max_active_sessions",
                "undertow_max_request_time_seconds",
                "undertow_min_request_time_seconds",
                "undertow_rejected_sessions_total",
                "undertow_request_time_seconds_total",
                "undertow_session_avg_alive_time_seconds",
                "undertow_session_max_alive_time_seconds",
                "undertow_sessions_created_total"
        );

        otelCollector.assertMetrics(prometheusMetrics -> {
            Map<String, PrometheusMetric> appMetrics =
                    prometheusMetrics.stream().filter(m -> m.getTags().entrySet().stream()
                                    .anyMatch(t -> "app".equals(t.getKey()) && DEPLOYMENT_NAME.equals(t.getValue()))
                            )
                            .collect(Collectors.toMap(PrometheusMetric::getKey, i -> i));

            metricsToTest.forEach(m -> Assert.assertTrue("Missing app metric: " + m, appMetrics.containsKey(m)));
        });
    }


    @Test
    @InSequence(5)
    public void testJmxMetrics() throws InterruptedException {
        List<String> metricsToTest = Arrays.asList(
                "thread_max_count",
                "classloader_loaded_classes",
                "cpu_system_load_average",
                "cpu_process_cpu_time",
                "classloader_loaded_classes_count",
                "thread_count",
                "thread_daemon_count",
                "cpu_available_processors"
        );

        otelCollector.assertMetrics(prometheusMetrics -> {
            metricsToTest.forEach(m -> {
                Assert.assertNotEquals("Metric value should be non-zero: " + m,
                        "0", prometheusMetrics.stream().filter(e -> e.getKey().startsWith(m))
                                .findFirst()
                                .orElseThrow()
                                .getValue()); // Add the metrics tags to complete the key
            });
        });
    }

    private Map<String, String> getMetricsMap(String response) {
        return Arrays.stream(response.split("\n"))
                .filter(s -> !s.startsWith("#"))
                .map(this::splitMetric)
                .collect(Collectors.toMap(e -> e[0], e -> e[1]));
    }

    private String[] splitMetric(String entry) {
        int index = entry.lastIndexOf(" ");
        return new String[] {
                entry.substring(0, index),
                entry.substring(index + 1)
        };
    }

    private String fetchMetrics(String nameToMonitor) throws InterruptedException {
        String body = "";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(otelCollector.getPrometheusUrl());

            int attemptCount = 0;
            boolean found = false;

            // Request counts can vary. Setting high to help ensure test stability
            while (!found && attemptCount < 30) {
                // Wait to give Micrometer time to export
                Thread.sleep(1000);

                body = target.request().get().readEntity(String.class);
                found = body.contains(nameToMonitor);
                attemptCount++;
            }
        }

        return body;
    }
}
