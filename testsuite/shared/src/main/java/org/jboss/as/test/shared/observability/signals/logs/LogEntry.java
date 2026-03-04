/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.logs;

import java.util.Map;

public record LogEntry(
        String traceId,
        String spanId,
        String body,
        long timeUnixNano,
        long observedTimeUnixNano,
        int severityNumber,
        String severityText,
        Map<String, String> attributes,
        int flags
) {
}
