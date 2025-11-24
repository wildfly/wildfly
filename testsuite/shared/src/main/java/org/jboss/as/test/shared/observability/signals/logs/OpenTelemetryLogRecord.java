/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.logs;

import java.util.Map;

public record OpenTelemetryLogRecord(
        String timeUnixNano,
        String observedTimeUnixNano,
        int severityNumber,
        String severityText,
        String body,
        Map<String, String> attributes,
        int flags,
        String traceId,
        String spanId
) {
}
