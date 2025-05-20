/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerSpan;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerTrace;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelMetricResource;

/**
 * This test verifies that two OpenTelemetry applications can be deployed to the same server while their
 * observability signals remain distinct. It deploys two applications and makes several requests to each to
 * generate data. It then verifies that traces for each application are successfully exported to the Collector.
 * Each trace will have a span with a tag of <code>url.path</code>. The value of that tag should be the name of
 * the archive (i.e., 'service-one' or 'service-two'). Similarly, there should be a counter exported for each
 * service with the tag <code>job</code> that contains the name of the archive. The value of the metric should
 * equal <code>REQUEST_COUNT</code>.
 */
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
@DockerRequired
public class OpenTelemetryMultipleWarTestCase extends BaseOpenTelemetryTest {
    protected static final String SERVICE_ONE = "service-one";
    protected static final String SERVICE_TWO = "service-two";
    private static final int REQUEST_COUNT = 5;

    @Deployment(name = SERVICE_ONE, order = 1, testable = false)
    public static WebArchive createDeployment1() {
        return buildBaseArchive(SERVICE_ONE);

    }

    @Deployment(name = SERVICE_TWO, order = 2, testable = false)
    public static WebArchive createDeployment2() {
        return buildBaseArchive(SERVICE_TWO);
    }

    @Test
    @InSequence(2)
    public void makeRequests() throws MalformedURLException {
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(SERVICE_ONE) + "?" + SERVICE_ONE).request().get().getStatus());
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(SERVICE_ONE) + "/metrics").request().get().getStatus());

                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(SERVICE_TWO) + "?" + SERVICE_TWO).request().get().getStatus());
                Assert.assertEquals(Response.Status.OK.getStatusCode(),
                    client.target(getDeploymentUrl(SERVICE_TWO) + "/metrics").request().get().getStatus());
            }
        }
    }

    @Test
    @InSequence(3)
    public void testTraces() throws InterruptedException {
        verifyTraces(SERVICE_ONE);
        verifyTraces(SERVICE_TWO);
    }

    @Test
    @InSequence(4)
    public void getMetrics() throws InterruptedException {
        verifyMetric(SERVICE_ONE);
        verifyMetric(SERVICE_TWO);
    }

    private void verifyTraces(String serviceName) throws InterruptedException {
        otelCollector.assertTraces(serviceName + ".war", traces -> {
            Assert.assertFalse(traces.isEmpty());

            Assert.assertTrue(traces.stream()
                .map(JaegerTrace::getSpans).flatMap(List::stream)
                .map(JaegerSpan::getTags).flatMap(List::stream)
                .anyMatch(t ->
                    Objects.equals(t.getKey(), "url.path") &&
                        Objects.equals(t.getValue(), "/" + serviceName + "/")));

        });
    }

    private void verifyMetric(String serviceName) throws InterruptedException {
        otelCollector.assertMetrics(prometheusMetrics -> {
            List<PrometheusMetric> results = otelCollector.getMetricsByName(prometheusMetrics,
                OtelMetricResource.COUNTER_NAME + "_total"); // Adjust for Prometheus naming conventions

            Assert.assertEquals(Integer.toString(REQUEST_COUNT),
                results.stream()
                    // Filter tag job=${serviceName}.war
                    .filter(metric ->
                        Objects.equals(metric.getTags().get("job"), serviceName + ".war")
                    )
                    .findFirst()
                    .orElseThrow(AssertionError::new)
                    .getValue());
        });
    }
}
