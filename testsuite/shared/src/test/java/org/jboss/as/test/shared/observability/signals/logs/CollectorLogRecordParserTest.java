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

    /** Verifies each resource block's attributes are attached only to its following log records. */
    @Test
    public void associatesResourceAttributesWithFollowingLogRecords() {
        String[] collectorOutput = {
                "ResourceLogs #0",
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
                "ResourceLogs #1",
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
