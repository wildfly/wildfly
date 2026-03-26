/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
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
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.collector.InMemoryCollector;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.as.test.shared.observability.signals.SimpleMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.JaxRsActivator;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@RunAsClient
public class MicrometerOtelIntegrationTestCase {
    public static final int REQUEST_COUNT = 5;
    public static final String DEPLOYMENT_NAME = "micrometer-test.war";
    private final InMemoryCollector collector = InMemoryCollector.getInstance();

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-test.war")
                .addClasses(JaxRsActivator.class, MicrometerResource.class);
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
    // a few metrics to verify their existence
    @Test
    @InSequence(4)
    public void getMetrics() throws InterruptedException {
        List<String> metricsToTest = Arrays.asList(
                "classloader_loaded_classes_count",
                "cpu_available_processors",
                "cpu_system_load_average",
                "demo_counter",
                "demo_timer",
                "gc_time",
                "jvm.classes.loaded",
                "memory_used_heap",
                "system.cpu.count",
                "thread_count",
                "undertow.bytes.received"
        );

        collector.assertMetrics(Duration.ofSeconds(10),
                metrics -> {
                    metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                            metrics.stream().anyMatch(m -> m.name().startsWith(n))));
                });
    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void testApplicationModelMetrics() throws InterruptedException {
        List<String> metricsToTest = List.of(
                "undertow.active.sessions",
                "undertow.expired.sessions",
                "undertow.highest.session.count",
                "undertow.max.active.sessions",
                "undertow.max.request.time",
                "undertow.min.request.time",
                "undertow.rejected.sessions",
                "undertow.total.request.time",
                "undertow.session.avg.alive.time",
                "undertow.session.max.alive.time",
                "undertow.sessions.created"
        );

        collector.assertMetrics(Duration.ofSeconds(10),
                metrics -> {
                    Map<String, SimpleMetric> appMetrics =
                            metrics.stream().filter(m -> m.tags().entrySet().stream()
                                            .anyMatch(t -> "app".equals(t.getKey()) && DEPLOYMENT_NAME.equals(t.getValue()))
                                    )
                                    .collect(Collectors.toMap(SimpleMetric::name, i -> i));

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

        collector.assertMetrics(metrics -> {
            metricsToTest.forEach(m -> {
                Assert.assertNotEquals("Metric value should be non-zero: " + m,
                        "0", metrics.stream().filter(e -> e.name().startsWith(m))
                                .findFirst()
                                .orElseThrow()
                                .value()); // Add the metrics tags to complete the key
            });
        });
    }
}
