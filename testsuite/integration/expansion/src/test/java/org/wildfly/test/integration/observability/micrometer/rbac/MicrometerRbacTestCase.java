/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.rbac;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.observability.MessagingSubsystemSetupTask;
import org.jboss.as.test.shared.observability.setuptasks.RbacRealmSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.test.integration.observability.JaxRsActivator;
import org.wildfly.test.integration.observability.micrometer.BaseMicrometerTest;
import org.wildfly.test.integration.observability.micrometer.MicrometerResource;
import org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ServerSetup({
        StabilityServerSetupSnapshotRestoreTasks.Community.class,
        PrometheusSetupTask.class,
        MessagingSubsystemSetupTask.class,
        RbacRealmSetupTask.class
})
public class MicrometerRbacTestCase extends BaseMicrometerTest {
    private static final String UNDERTOW_MODEL_METRIC = "undertow_bytes_sent";
    private static final String QUEUE_NAME = "micrometer-resource-add-remove-rbac";
    private static final String QUEUE_METRIC = "messaging_activemq_message_count";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "micrometer-rbac.war")
                .addClasses(JaxRsActivator.class, MicrometerResource.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Test
    public void testPullMicrometerWithAuthentication() throws Exception {
        verifyPullMetrics(true);
    }

    @Test
    public void testPullMicrometerWithoutAuthentication() throws Exception {
        verifyPullMetrics(false);
    }

    /**
     * Negative test: with RBAC and endpoint security enabled, an unauthenticated scrape of the Prometheus pull
     * endpoint must be rejected outright rather than served zeroed-out (anonymous) metrics.
     */
    @Test
    public void testPullMicrometerUnauthenticatedRejected() throws Exception {
        setAuthenticationEnabled(true);

        Assertions.assertEquals(401,
                fetchPrometheusStatus(null, null), "Unauthenticated scrape of the secured Prometheus endpoint should be rejected");
    }

    /**
     * Negative test: an authenticated management user without at least the Monitor role must not see the
     * RBAC-protected model metrics, including the metric series labels.
     */
    @Test
    public void testPullMicrometerWithoutMonitorRole() throws Exception {
        setAuthenticationEnabled(true);
        makeRequests();

        List<PrometheusMetric> metrics =
                fetchPrometheusMetrics(RbacRealmSetupTask.USER_NOROLE, RbacRealmSetupTask.TEST_RBAC_PASSWORD);

        Assertions.assertFalse(metrics.stream().anyMatch(metric ->
                        metric.getKey().startsWith(UNDERTOW_MODEL_METRIC)),
                "The Undertow model metric series must be omitted for a user without the Monitor role");
    }

    /** Verifies dynamic resource discovery still registers Micrometer metrics when RBAC is enabled. */
    @Test
    public void testDynamicResourceMetricsWithAuthentication() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        boolean queueCreated = false;
        try {
            setAuthenticationEnabled(true);
            jmsOperations.createJmsQueue(QUEUE_NAME, "java:/jms/queue/" + QUEUE_NAME);
            queueCreated = true;

            Awaitility.await("Micrometer did not register the dynamically added JMS queue under RBAC")
                    .pollInterval(TimeoutUtil.adjust(Duration.ofSeconds(1)))
                    .atMost(TimeoutUtil.adjust(Duration.ofSeconds(20)))
                    .untilAsserted(() -> assertQueueMetric(true));

            List<PrometheusMetric> unauthorizedMetrics = fetchPrometheusMetrics(
                    RbacRealmSetupTask.USER_NOROLE, RbacRealmSetupTask.TEST_RBAC_PASSWORD);
            Assertions.assertFalse(unauthorizedMetrics.stream().anyMatch(metric -> QUEUE_METRIC.equals(metric.getKey())
                            && "jms-queue".equals(metric.getTags().get("type"))
                            && QUEUE_NAME.equals(metric.getTags().get("name"))),
                    "The dynamically added JMS queue series and its name label must be omitted without access");

            jmsOperations.removeJmsQueue(QUEUE_NAME);
            queueCreated = false;
            Awaitility.await("Micrometer did not remove the JMS queue metric under RBAC")
                    .pollInterval(TimeoutUtil.adjust(Duration.ofSeconds(1)))
                    .atMost(TimeoutUtil.adjust(Duration.ofSeconds(20)))
                    .untilAsserted(() -> assertQueueMetric(false));
        } finally {
            if (queueCreated) {
                jmsOperations.removeJmsQueue(QUEUE_NAME);
            }
            jmsOperations.close();
            setAuthenticationEnabled(false);
        }
    }

    /**
     * Verifies the push path: metrics exported over OTLP to the OpenTelemetry Collector carry real model values under
     * RBAC. This exercises the background push scheduler, which - unlike a scrape - has no caller identity to filter by,
     * so {@code WildFlyOtlpRegistry.publish()} reads the model as an in-VM (SuperUser) call. Without that the model
     * metrics would be read as the anonymous identity and zeroed out under RBAC.
     */
    @Test
    public void testPushMicrometer() throws Exception {
        setAuthenticationEnabled(true);

        // The collector's exported series is cumulative and retains the last value it received for several minutes.
        // Asserting merely value>0 would therefore pass on model metrics pushed before RBAC was enabled - i.e. even
        // if publish() now read the model as the anonymous identity and stopped pushing real values. Baseline the
        // metric with RBAC already on, drive fresh traffic, then require a strict increase: that can only happen if a
        // post-RBAC in-VM (SuperUser) push actually lands a real model value.
        final double baseline = currentPushedUndertowValue();

        makeRequests();

        otelCollector.assertMetrics(metrics -> {
            PrometheusMetric undertowMetric = metrics.stream()
                    .filter(metric -> metric.getKey().startsWith(UNDERTOW_MODEL_METRIC))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("The Undertow model metric '" + UNDERTOW_MODEL_METRIC +
                            "' was not pushed to the collector under RBAC"));
            double value = Double.parseDouble(undertowMetric.getValue());
            Assertions.assertTrue(value > baseline, "The Undertow model metric '" + UNDERTOW_MODEL_METRIC + "' must increase after RBAC is " +
                    "enabled (baseline " + baseline + "), but the collector still shows " + value);
        });
    }

    /**
     * Current value the collector holds for the Undertow model metric, or {@code 0.0} if it has not been pushed yet.
     * Used to baseline {@link #testPushMicrometer()} against stale, pre-RBAC values retained by the collector.
     */
    private double currentPushedUndertowValue() {
        return otelCollector.fetchMetrics().stream()
                .filter(metric -> metric.getKey().startsWith(UNDERTOW_MODEL_METRIC))
                .findFirst()
                .map(metric -> Double.parseDouble(metric.getValue()))
                .orElse(0.0);
    }

    private void verifyPullMetrics(boolean authenticationEnabled) throws Exception {
        setAuthenticationEnabled(authenticationEnabled);
        makeRequests();

        assertMetric(authenticationEnabled,
                authenticationEnabled
                        ? fetchPrometheusMetrics(RbacRealmSetupTask.USER_MONITOR, RbacRealmSetupTask.TEST_RBAC_PASSWORD)
                        : fetchPrometheusMetrics(null, null));
    }

    private void assertMetric(boolean authenticationEnabled, List<PrometheusMetric> metrics) {
        PrometheusMetric undertowMetric = metrics.stream().filter(metric ->
                        metric.getKey().startsWith(UNDERTOW_MODEL_METRIC)).findFirst()
                .orElseThrow(() -> new AssertionError("Undertow model metric not found"));

        double value = Double.parseDouble(undertowMetric.getValue());

        Assertions.assertTrue(value > 0, "The Undertow model metric '" + UNDERTOW_MODEL_METRIC +
                "' should be non-zero (authentication " + (authenticationEnabled ? "enabled" : "disabled") +
                "), but was " + value);
    }

    /** Asserts whether the queue's runtime metric is present in an authenticated scrape. */
    private void assertQueueMetric(boolean expected) throws Exception {
        boolean found = fetchPrometheusMetrics(RbacRealmSetupTask.USER_MONITOR,
                RbacRealmSetupTask.TEST_RBAC_PASSWORD).stream()
                .anyMatch(metric -> QUEUE_METRIC.equals(metric.getKey())
                        && "jms-queue".equals(metric.getTags().get("type"))
                        && QUEUE_NAME.equals(metric.getTags().get("name")));
        Assertions.assertEquals(expected, found, "Unexpected JMS queue metric state under RBAC");
    }

    /**
     * Enables or disables RBAC and, in lockstep, the security of the Micrometer Prometheus endpoints, then reloads.
     * With authentication enabled, scrapes must present a management-realm identity with at least Monitor access.
     * With it disabled, the endpoints are open.
     */
    private void setAuthenticationEnabled(boolean enabled) throws Exception {
        executeOperation(Util.createCompositeOperation(
            List.of(
                // /core-service=management/access=authorization:write-attribute(name=provider,value=rbac|simple)
                Util.getWriteAttributeOperation(RbacRealmSetupTask.authorization, PROVIDER, enabled ? "rbac" : "simple"),
                // /subsystem=micrometer/registry=prometheus:write-attribute(name=security-enabled,value=...)
                Util.getWriteAttributeOperation(PathAddress.pathAddress(SUBSYSTEM, "micrometer")
                        .append("registry", "prometheus"), "security-enabled", enabled)
            )));
        ServerReload.reloadIfRequired(managementClient);
    }
}
