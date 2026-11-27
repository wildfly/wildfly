/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.micrometer.metrics.MetricMetadata;
import org.wildfly.extension.micrometer.metrics.WildFlyMetric;
import org.wildfly.extension.micrometer.metrics.WildFlyMetricMetadata;

/**
 * Verifies that a Prometheus scrape omits model series (and their labels) whose backing resource read was
 * denied for the scraping caller, while unrelated series in the same metric family remain visible.
 */
public class WildFlyPrometheusRegistryTest {

    private static final String ATTRIBUTE = "message-count";
    private static final String SUBSYSTEM = "messaging-activemq";
    private static final String ALLOWED_QUEUE = "allowedQueue";
    private static final String DENIED_QUEUE = "deniedQueue";

    /**
     * Registers two JMS-queue counters sharing a metric family: one whose read succeeds and one whose read is
     * denied under RBAC. The scrape must expose the allowed series and drop the denied series together with its
     * {@code name} label, exercising counter naming ({@code _total}) and empty-value label matching.
     */
    @Test
    public void scrapeOmitsDeniedSeriesAndLabels() {
        try (WildFlyPrometheusRegistry registry = new WildFlyPrometheusRegistry()) {
            registry.addMeter(metricFor(ALLOWED_QUEUE, allowingClient()), metadataFor(ALLOWED_QUEUE));
            registry.addMeter(metricFor(DENIED_QUEUE, denyingClient()), metadataFor(DENIED_QUEUE));

            String scrape = registry.scrape();

            assertTrue("The authorized JMS queue series must remain visible",
                    scrape.contains("name=\"" + ALLOWED_QUEUE + "\""));
            assertFalse("The denied JMS queue series and its name label must be omitted",
                    scrape.contains(DENIED_QUEUE));
        }
    }

    /** Verifies that a denied gauge with a bytes unit is removed without hiding its authorized sibling. */
    @Test
    public void scrapeOmitsDeniedGaugeWithBytesUnit() {
        assertDeniedSeriesOmitted("queue-size", MeasurementUnit.BYTES, MetricMetadata.Type.GAUGE);
    }

    /** Verifies that a denied gauge with a time unit is removed without hiding its authorized sibling. */
    @Test
    public void scrapeOmitsDeniedGaugeWithMillisecondsUnit() {
        assertDeniedSeriesOmitted("queue-age", MeasurementUnit.MILLISECONDS, MetricMetadata.Type.GAUGE);
    }

    /** Verifies that a denied counter whose source name has a reserved suffix is removed correctly. */
    @Test
    public void scrapeOmitsDeniedCounterWithReservedSuffix() {
        assertDeniedSeriesOmitted("request-total", MeasurementUnit.NONE, MetricMetadata.Type.COUNTER);
    }

    /** Verifies that a denied metric with punctuation requiring sanitization is removed correctly. */
    @Test
    public void scrapeOmitsDeniedMetricWithSanitizedName() {
        assertDeniedSeriesOmitted("queue.size", MeasurementUnit.NONE, MetricMetadata.Type.GAUGE);
    }

    /** Verifies filtering uses the Prometheus name when snapshot metadata retains punctuation. */
    @Test
    public void filtersDeniedSnapshotUsingPrometheusName() throws Exception {
        Meter.Id deniedMeter = new Meter.Id("queue.size",
                Tags.of(Tag.of("type", "jms-queue"), Tag.of("name", DENIED_QUEUE),
                        Tag.of("deployment", ""), Tag.of("subdeployment", ""), Tag.of("app", "wildfly")),
                null, "queue size", Meter.Type.GAUGE);
        MetricSnapshots snapshots = MetricSnapshots.of(
                new GaugeSnapshot(new io.prometheus.metrics.model.snapshots.MetricMetadata("queue.size"),
                        List.of(new GaugeSnapshot.GaugeDataPointSnapshot(7,
                                Labels.of(List.of("type", "name", "deployment", "subdeployment", "app"),
                                        List.of("jms-queue", DENIED_QUEUE, "", "", "wildfly")), null))));

        Method filter = WildFlyPrometheusRegistry.class.getDeclaredMethod("filterDeniedSeries",
                MetricSnapshots.class, Set.class);
        filter.setAccessible(true);

        MetricSnapshots filtered = (MetricSnapshots) filter.invoke(null, snapshots, Set.of(deniedMeter));

        assertEquals("The denied snapshot must be omitted after Prometheus name sanitization", 0, filtered.size());
    }

    /** Verifies denied-series filtering for one metric family while retaining the authorized series. */
    private static void assertDeniedSeriesOmitted(String attribute, MeasurementUnit unit,
                                                   MetricMetadata.Type type) {
        try (WildFlyPrometheusRegistry registry = new WildFlyPrometheusRegistry()) {
            registry.addMeter(metricFor(ALLOWED_QUEUE, attribute, allowingClient()),
                    metadataFor(ALLOWED_QUEUE, attribute, unit, type));
            registry.addMeter(metricFor(DENIED_QUEUE, attribute, denyingClient()),
                    metadataFor(DENIED_QUEUE, attribute, unit, type));

            String scrape = registry.scrape();

            assertTrue("The authorized series must remain visible",
                    scrape.contains("name=\"" + ALLOWED_QUEUE + "\""));
            assertFalse("The denied series and its name label must be omitted",
                    scrape.contains(DENIED_QUEUE));
        }
    }

    /** Builds a model metric for a named JMS queue backed by the given management client. */
    private static WildFlyMetric metricFor(String queueName, LocalModelControllerClient client) {
        return metricFor(queueName, ATTRIBUTE, client);
    }

    /** Builds a model metric for a named JMS queue and attribute backed by the given management client. */
    private static WildFlyMetric metricFor(String queueName, String attribute,
                                           LocalModelControllerClient client) {
        return new WildFlyMetric(client, addressFor(queueName), attribute);
    }

    /** Builds counter metadata matching the address of a named JMS queue. */
    private static MetricMetadata metadataFor(String queueName) {
        return metadataFor(queueName, ATTRIBUTE, MeasurementUnit.NONE, MetricMetadata.Type.COUNTER);
    }

    /** Builds metric metadata matching the address, attribute, unit, and type of a named JMS queue. */
    private static MetricMetadata metadataFor(String queueName, String attribute, MeasurementUnit unit,
                                              MetricMetadata.Type type) {
        return new WildFlyMetricMetadata(attribute, addressFor(queueName), "messages on the queue", unit, type);
    }

    /** Builds the management address {@code subsystem=messaging-activemq/jms-queue=<name>}. */
    private static PathAddress addressFor(String queueName) {
        return PathAddress.pathAddress(
                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM),
                PathElement.pathElement("jms-queue", queueName));
    }

    /** Returns a client whose attribute read succeeds with a numeric value. */
    private static LocalModelControllerClient allowingClient() {
        ModelNode response = new ModelNode();
        response.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
        response.get(ModelDescriptionConstants.RESULT).set(7L);
        return new FixedResponseClient(response);
    }

    /** Returns a client whose attribute read fails with an RBAC authorization error. */
    private static LocalModelControllerClient denyingClient() {
        ModelNode response = new ModelNode();
        response.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.FAILED);
        response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION)
                .set("WFLYCTL0216: Management resource not found");
        return new FixedResponseClient(response);
    }

    /** Minimal {@link LocalModelControllerClient} that returns one canned response for every {@code execute}. */
    private static final class FixedResponseClient implements LocalModelControllerClient {
        private final ModelNode response;

        private FixedResponseClient(ModelNode response) {
            this.response = response;
        }

        @Override
        public ModelNode execute(ModelNode operation) {
            return response;
        }

        @Override
        public OperationResponse executeOperation(Operation operation, OperationMessageHandler messageHandler) {
            return OperationResponse.Factory.createSimple(response);
        }

        @Override
        public CompletableFuture<ModelNode> executeAsync(Operation operation, OperationMessageHandler messageHandler) {
            return CompletableFuture.completedFuture(response);
        }

        @Override
        public CompletableFuture<OperationResponse> executeOperationAsync(Operation operation,
                                                                          OperationMessageHandler messageHandler) {
            return CompletableFuture.completedFuture(OperationResponse.Factory.createSimple(response));
        }

        @Override
        public void close() {
        }
    }
}
