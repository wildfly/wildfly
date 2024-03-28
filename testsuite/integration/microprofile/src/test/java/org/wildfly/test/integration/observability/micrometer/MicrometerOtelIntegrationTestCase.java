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
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
public class MicrometerOtelIntegrationTestCase {
    protected static boolean dockerAvailable = AssumeTestGroupUtil.isDockerAvailable();

    public static final int REQUEST_COUNT = 5;
    @ArquillianResource
    private URL url;
    @Inject
    private MeterRegistry meterRegistry;

    static final String WEB_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
                    + "           xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
                    + "         xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\" \n"
                    + "              version=\"4.0\">\n"
                    + "    <servlet-mapping>\n"
                    + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
                    + "        <url-pattern>/*</url-pattern>\n"
                    + "    </servlet-mapping>"
                    + "</web-app>\n";

    @Deployment
    public static Archive<?> deploy() {
        return dockerAvailable ?
                ShrinkWrap.create(WebArchive.class, "micrometer-test.war")
                        .addClasses(ServerSetupTask.class,
                                MetricResource.class,
                                AssumeTestGroupUtil.class)
                        .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                        .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml") :
                AssumeTestGroupUtil.emptyWar();
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
    public void testInjection() {
        Assert.assertNotNull(meterRegistry);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void makeRequests() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < REQUEST_COUNT; i++) {
                target.request().get();
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkCounter() {
        Counter counter = meterRegistry.get("demo_counter").counter();
        Assert.assertEquals(counter.count(), REQUEST_COUNT, 0.0);
    }

    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify there existence
    @Test
    @RunAsClient
    @InSequence(4)
    public void getMetrics() throws InterruptedException {
        List<String> metricsToTest = Arrays.asList(
                "demo_counter",
                "memory_used_heap",
                "cpu_available_processors",
                "classloader_loaded_classes_count",
                "cpu_system_load_average",
                "gc_time",
                "thread_count",
                "undertow_bytes_received"
        );

        final String response = fetchMetrics(metricsToTest.get(0));
        metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n, response.contains(n)));
    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void testJmxMetrics() throws InterruptedException {
        List<String> metricsToTest = Arrays.asList(
                "thread_max_count",
                "classloader_loaded_classes",
                "cpu_system_load_average",
                "cpu_process_cpu_time",
                "classloader_unloaded_classes",
                "classloader_loaded_classes_count",
                "thread_count",
                "thread_daemon_count",
                "cpu_available_processors"
        );
        final String response = fetchMetrics(metricsToTest.get(0));
        Map<String, String> metrics = Arrays.stream(response.split("\n"))
                .filter(s -> !s.startsWith("#"))
                .map(this::splitMetric)
                .collect(Collectors.toMap(e -> e[0], e -> e[1]));
        metricsToTest.forEach(m -> {
            Assert.assertNotEquals("Metric value should be non-zero: " + m,
                    "0", metrics.get(m + "{job=\"wildfly\"}")); // Add the metrics tags to complete the key
        });
    }

    private String[] splitMetric(String entry) {
        int index = entry.lastIndexOf(" ");
        return new String[]{
                entry.substring(0, index),
                entry.substring(index + 1)
        };
    }

    private String fetchMetrics(String nameToMonitor) throws InterruptedException {
        String body = "";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(OpenTelemetryCollectorContainer.getInstance().getPrometheusUrl());

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
