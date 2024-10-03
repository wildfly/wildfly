/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.microprofile.telemetry;

import static org.wildfly.test.preview.microprofile.telemetry.application.MetricResource.COUNTER_NAME;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

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
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.preview.microprofile.telemetry.application.JaxRsActivator;
import org.wildfly.test.preview.microprofile.telemetry.application.MetricResource;

@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
@DockerRequired(AssumptionViolatedException.class)
@RunAsClient
public class MetricsTestCase {
    public static final int REQUEST_COUNT = 5;
    public static final String DEPLOYMENT_NAME = "metrics-test.war";

    @ArquillianResource
    private URL url;

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .addClasses(JaxRsActivator.class, MetricResource.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
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
    @InSequence(2)
    public void getMetrics() throws InterruptedException {
        List<String> metricsToTest = List.of(
                COUNTER_NAME
        );

        final List<PrometheusMetric> metrics = otelCollector.fetchMetrics(metricsToTest.get(0));
        metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                metrics.stream().anyMatch(m -> m.getKey().startsWith(n))));
    }
}
