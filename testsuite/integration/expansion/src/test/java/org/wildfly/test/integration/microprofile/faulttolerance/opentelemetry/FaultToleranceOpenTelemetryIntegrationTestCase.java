/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.faulttolerance.opentelemetry;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.FaultToleranceMicrometerIntegrationTestCase;
import org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment.FaultTolerantApplication;

/**
 * Test case to verify basic SmallRye Fault Tolerance integration with MicroProfile Telemetry Metrics.
 * The test first invokes a REST application which always times out, and Eclipse MP FT @Timeout kicks in with a fallback.
 * Then we verify several of the counters collected by OTEL via Prometheus.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@RunAsClient
@TestcontainersRequired
@org.junit.Ignore
// This test case does not use Micrometer *but* we enable it to verify CompoundMetricsProvider functionality in WF
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class, MicrometerSetupTask.class})
public class FaultToleranceOpenTelemetryIntegrationTestCase {

    @Testcontainer
    protected OpenTelemetryCollectorContainer otelCollector;

    private static final String MP_CONFIG = "otel.sdk.disabled=false\n" +
            // Lower the interval from 60 seconds to 100 millis
            "otel.metric.export.interval=100";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, FaultToleranceMicrometerIntegrationTestCase.class.getSimpleName() + ".war")
                .addAsManifestResource(new StringAsset(MP_CONFIG), "microprofile-config.properties")
                .addPackage(FaultTolerantApplication.class.getPackage())
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(FaultToleranceMicrometerIntegrationTestCase.class.getPackage(), "beans.xml", "beans.xml");
    }

    // Let's use a slightly higher number of invocations, so we can at times differentiate between stale read and other problems
    private static final int INVOCATION_COUNT = 10;

    @ArquillianResource
    private URL url;

    @Test
    public void test() throws Exception {
        // First make requests
        for (int i = 0; i < INVOCATION_COUNT; i++) {
            HttpRequest.get(url.toString() + "app/timeout", 10, TimeUnit.SECONDS);
        }

        otelCollector.assertMetrics(metrics -> {
            Optional<PrometheusMetric> prometheusMetric;

            // First verify total invocation count for the method + value returned + fallback applied
            prometheusMetric = metrics.stream().filter(metric -> metric.getKey().equals("ft_invocations_total")).findFirst();
            Assert.assertTrue(prometheusMetric.isPresent());
            Assert.assertEquals(INVOCATION_COUNT, Integer.parseInt(prometheusMetric.get().getValue()), 0);


            // Verify the number of timeouts being equal to the number of invocations
            prometheusMetric = metrics.stream()
                    .filter(metric -> metric.getKey().equals("ft_timeout_calls_total"))
                    .filter(metric -> Boolean.TRUE.toString().equalsIgnoreCase(metric.getTags().get("timedOut")))
                    .findFirst();
            Assert.assertTrue(prometheusMetric.isPresent());
            Assert.assertEquals(INVOCATION_COUNT, Integer.parseInt(prometheusMetric.get().getValue()), 0);


            // Verify the number of successful invocations to be none, since it always fails
            prometheusMetric = metrics.stream()
                    .filter(metric -> metric.getKey().equals("ft_timeout_calls_total"))
                    .filter(metric -> Boolean.FALSE.toString().equalsIgnoreCase(metric.getTags().get("timedOut")))
                    .findFirst();
            Assert.assertTrue(prometheusMetric.isPresent());
            Assert.assertEquals(0, Integer.parseInt(prometheusMetric.get().getValue()), 0);
        }, Duration.ofSeconds(70)); // n.b. this in ~2% of cases needs slightly more time on our CI given the asynchronicity
    }

}
