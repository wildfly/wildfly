/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.jaeger;

import java.util.List;
import java.util.Map;

@Deprecated
public class JaegerTrace {
    private String traceID;
    private List<JaegerSpan> spans;
    private Map<String, JaegerProcess> processes;
    private List<String> warnings;

    public String getTraceID() {
        return traceID;
    }

    public JaegerTrace setTraceID(String traceID) {
        this.traceID = traceID;
        return this;
    }

    public List<JaegerSpan> getSpans() {
        return spans;
    }

    public JaegerTrace setSpans(List<JaegerSpan> spans) {
        this.spans = spans;
        return this;
    }

    public Map<String, JaegerProcess> getProcesses() {
        return processes;
    }

    public JaegerTrace setProcesses(Map<String, JaegerProcess> processes) {
        this.processes = processes;
        return this;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public JaegerTrace setWarnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    @Override
    public String toString() {
        return "JaegerTrace{" +
                "traceID='" + traceID + '\'' +
                ", spans=" + spans +
                ", processes=" + processes +
                ", warnings=" + warnings +
                '}';
    }
}
