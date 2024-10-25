/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.MetricResource;

@ServerSetup(OpenTelemetryWithCollectorSetupTask.class)
@RunAsClient
public class MetricsTestCase extends BaseOpenTelemetryTest {
    private static final int REQUEST_COUNT = 5;
    private static final String DEPLOYMENT_NAME = "metrics-test";

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    @Test
    @InSequence(1)
    public void makeRequests() throws MalformedURLException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(getDeploymentUrl(DEPLOYMENT_NAME));
            for (int i = 0; i < REQUEST_COUNT; i++) {
                target.request().get();
            }
        }
    }

    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify there existence
    @Test
    @InSequence(2)
    public void getMetrics() throws InterruptedException {
        List<String> metricsToTest = List.of(
                MetricResource.COUNTER_NAME
        );

        final List<PrometheusMetric> metrics = otelCollector.fetchMetrics(metricsToTest.get(0));
        metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                metrics.stream().anyMatch(m -> m.getKey().startsWith(n))));
    }
}
