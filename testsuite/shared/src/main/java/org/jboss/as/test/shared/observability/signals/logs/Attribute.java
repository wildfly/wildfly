/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.logs;

public record Attribute(
        String key,
        OpenTelemetryLogRecord.Value value
) {
}
