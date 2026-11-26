/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.logs;

import java.util.Map;

/**
 * Immutable representation of one log record parsed from OpenTelemetry Collector output.
 *
 * @param timeUnixNano the event timestamp in Unix nanoseconds
 * @param observedTimeUnixNano the observed timestamp in Unix nanoseconds
 * @param severityNumber the numeric OpenTelemetry severity
 * @param severityText the textual severity
 * @param body the log body
 * @param resourceAttributes the enclosing resource attributes
 * @param attributes the log record attributes
 * @param flags the OpenTelemetry trace flags
 * @param traceId the associated trace identifier
 * @param spanId the associated span identifier
 */
public record OpenTelemetryLogRecord(
        String timeUnixNano,
        String observedTimeUnixNano,
        int severityNumber,
        String severityText,
        String body,
        Map<String, String> resourceAttributes,
        Map<String, String> attributes,
        int flags,
        String traceId,
        String spanId
) {
    /**
     * Creates a log record while snapshotting attribute maps so the record remains immutable.
     */
    public OpenTelemetryLogRecord {
        resourceAttributes = Map.copyOf(resourceAttributes);
        attributes = Map.copyOf(attributes);
    }
}
