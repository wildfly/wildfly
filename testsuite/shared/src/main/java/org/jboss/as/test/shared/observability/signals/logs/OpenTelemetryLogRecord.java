/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.logs;

import java.util.List;

public record OpenTelemetryLogRecord(
        String timeUnixNano,
        String observedTimeUnixNano,
        int severityNumber,
        String severityText,
        Body body,
        List<Attribute> attributes,
        int flags,
        String traceId,
        String spanId
) {
    public record Body(
            String stringValue
    ) {}

    public record Value(
            String stringValue
    ) {}
}
