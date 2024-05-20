/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
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
import org.wildfly.test.integration.observability.arquillian.TestContainer;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.container.PrometheusMetric;
import org.wildfly.test.integration.observability.setuptask.MicrometerSetupTask;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@RunAsClient
@TestContainer(OpenTelemetryCollectorContainer.class)
public class MicrometerOtelIntegrationTestCase {
    public static final int REQUEST_COUNT = 5;
    @ArquillianResource
    private URL url;
    @Inject
    private MeterRegistry meterRegistry;
    @ArquillianResource
    OpenTelemetryCollectorContainer otelContainer;

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
        return ShrinkWrap.create(WebArchive.class, "micrometer-test.war")
                .addClasses(ServerSetupTask.class,
                        MetricResource.class,
                        AssumeTestGroupUtil.class,
                        PrometheusMetric.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    // The @ServerSetup(MicrometerSetupTask.class) requires Docker to be available.
    // Otherwise the org.wildfly.extension.micrometer.registry.NoOpRegistry is installed which will result in 0 counters,
    // and cause the test fail seemingly intermittently on machines with broken Docker setup.
    @BeforeClass
    public static void checkForDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }

//    @Test
//    @InSequence(1)
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

        final List<PrometheusMetric> metrics = otelContainer.fetchMetrics(metricsToTest.get(0));
        metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                metrics.stream().anyMatch(m -> m.getKey().startsWith(n))));
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
        final List<PrometheusMetric> metrics = otelContainer.fetchMetrics(metricsToTest.get(0));

        metricsToTest.forEach(m -> {
            Assert.assertNotEquals("Metric value should be non-zero: " + m,
                    "0", metrics.stream().filter(e -> e.getKey().startsWith(m))
                            .findFirst()
                            .orElseThrow()
                            .getValue()); // Add the metrics tags to complete the key
        });
    }
}
