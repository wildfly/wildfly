/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.OptionalDouble;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.junit.Test;

/**
 * Verifies that the Prometheus exporter omits a model series entirely (name and labels) when its value cannot be
 * read, as happens when a scraping caller lacks RBAC access to the backing resource.
 */
public class PrometheusExporterTest {

    private static final String ATTRIBUTE = "message-count";
    private static final String SUBSYSTEM = "messaging-activemq";
    private static final String ALLOWED_QUEUE = "allowedQueue";
    private static final String DENIED_QUEUE = "deniedQueue";

    /**
     * Registers two JMS-queue metrics sharing a family: one whose read succeeds and one whose read is denied
     * (an empty value, mirroring an RBAC denial). The export must contain the allowed series and must not leak
     * the denied series or its {@code name} label.
     */
    @Test
    public void exportOmitsUnreadableSeriesAndLabels() {
        try (WildFlyMetricRegistry registry = new WildFlyMetricRegistry()) {
            registry.registerMetric(() -> OptionalDouble.of(7), metadataFor(ALLOWED_QUEUE));
            registry.registerMetric(OptionalDouble::empty, metadataFor(DENIED_QUEUE));

            String export = new PrometheusExporter().export(registry);

            assertTrue("The readable JMS queue series must be exported",
                    export.contains("\"" + ALLOWED_QUEUE + "\""));
            assertFalse("The unreadable JMS queue series and its address-derived label must be omitted",
                    export.contains(DENIED_QUEUE));
        }
    }

    /** Builds gauge metadata for the address {@code subsystem=messaging-activemq/jms-queue=<name>}. */
    private static MetricMetadata metadataFor(String queueName) {
        PathAddress address = PathAddress.pathAddress(
                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM),
                PathElement.pathElement("jms-queue", queueName));
        return new WildFlyMetricMetadata(ATTRIBUTE, address, null, "messages on the queue",
                MeasurementUnit.NONE, MetricMetadata.Type.GAUGE);
    }
}
