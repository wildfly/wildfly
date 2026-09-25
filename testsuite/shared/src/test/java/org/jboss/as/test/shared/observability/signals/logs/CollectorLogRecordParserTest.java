/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared.observability.signals.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests resource-level attribute association in {@link CollectorLogRecordParser}.
 */
public class CollectorLogRecordParserTest {

    /** Verifies the collector's singular resource header is parsed with its resource attributes. */
    @Test
    public void parsesSingularResourceLogHeaders() {
        String[] collectorOutput = {
                "[OpenTelemetryCollector] ResourceLog #0",
                "[OpenTelemetryCollector] Resource attributes:",
                "[OpenTelemetryCollector]      -> service.name: Str(service-one)",
                "[OpenTelemetryCollector] LogRecord #0",
                "[OpenTelemetryCollector] Timestamp: 2026-09-24 18:00:00.000000 +0000 UTC",
                "[OpenTelemetryCollector] ObservedTimestamp: 2026-09-24 18:00:00.000000 +0000 UTC",
                "[OpenTelemetryCollector] SeverityText: INFO",
                "[OpenTelemetryCollector] SeverityNumber: Info(9)",
                "[OpenTelemetryCollector] Body: Str(first)",
                "[OpenTelemetryCollector] Flags: 0",
                "[OpenTelemetryCollector] LogRecord #1",
                "[OpenTelemetryCollector] Timestamp: 2026-09-24 18:00:00.000000 +0000 UTC",
                "[OpenTelemetryCollector] ObservedTimestamp: 2026-09-24 18:00:00.000000 +0000 UTC",
                "[OpenTelemetryCollector] SeverityText: INFO",
                "[OpenTelemetryCollector] SeverityNumber: Info(9)",
                "[OpenTelemetryCollector] Body: Str(first-second)",
                "[OpenTelemetryCollector] Flags: 0",
                "[OpenTelemetryCollector] ResourceLog #1",
                "[OpenTelemetryCollector] Resource attributes:",
                "[OpenTelemetryCollector]      -> service.name: Str(service-two)",
                "[OpenTelemetryCollector] LogRecord #0",
                "[OpenTelemetryCollector] Timestamp: 2026-09-24 18:00:01.000000 +0000 UTC",
                "[OpenTelemetryCollector] ObservedTimestamp: 2026-09-24 18:00:01.000000 +0000 UTC",
                "[OpenTelemetryCollector] SeverityText: INFO",
                "[OpenTelemetryCollector] SeverityNumber: Info(9)",
                "[OpenTelemetryCollector] Body: Str(second)",
                "[OpenTelemetryCollector] Flags: 0",
                "[OpenTelemetryCollector] {\"resource\":{}}"
        };

        List<OpenTelemetryLogRecord> records = new CollectorLogRecordParser().retrieveLogRecords(collectorOutput);

        assertEquals(3, records.size(), "The singular resource-log records should be parsed");
        assertEquals("first", records.get(0).body());
        assertEquals("service-one", records.get(0).resourceAttributes().get("service.name"));
        assertEquals("first-second", records.get(1).body());
        assertEquals("service-one", records.get(1).resourceAttributes().get("service.name"));
        assertEquals("second", records.get(2).body());
        assertEquals("service-two", records.get(2).resourceAttributes().get("service.name"));
    }

    /** Verifies each resource block's attributes are attached only to its following log records. */
    @Test
    public void associatesResourceAttributesWithFollowingLogRecords() {
        String[] collectorOutput = {
                "ResourceLog #0",
                "Resource attributes:",
                "     -> service.name: Str(service-one)",
                "ScopeLogs #0",
                "LogRecord #0",
                "Timestamp: 2026-09-24 18:00:00.000000 +0000 UTC",
                "ObservedTimestamp: 2026-09-24 18:00:00.000000 +0000 UTC",
                "SeverityText: INFO",
                "SeverityNumber: Info(9)",
                "Body: Str(first)",
                "Flags: 0",
                "{\"resource\":{}}",
                "ResourceLog #1",
                "Resource attributes:",
                "     -> service.name: Str(service-two)",
                "ScopeLogs #0",
                "LogRecord #0",
                "Timestamp: 2026-09-24 18:00:01.000000 +0000 UTC",
                "ObservedTimestamp: 2026-09-24 18:00:01.000000 +0000 UTC",
                "SeverityText: INFO",
                "SeverityNumber: Info(9)",
                "Body: Str(second)",
                "Flags: 0",
                "{\"resource\":{}}"
        };

        List<OpenTelemetryLogRecord> records = new CollectorLogRecordParser().retrieveLogRecords(collectorOutput);

        assertEquals("service-one", records.get(0).resourceAttributes().get("service.name"),
                "First log record should carry the first resource block's service.name");
        assertEquals("service-two", records.get(1).resourceAttributes().get("service.name"),
                "Second log record should carry the second resource block's service.name");
    }
}
