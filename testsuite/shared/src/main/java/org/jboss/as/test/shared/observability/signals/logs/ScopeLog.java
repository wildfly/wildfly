/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.logs;

import java.util.List;

public class ScopeLog {
    public Scope scope;
    public List<OpenTelemetryLogRecord> logRecords;
}
