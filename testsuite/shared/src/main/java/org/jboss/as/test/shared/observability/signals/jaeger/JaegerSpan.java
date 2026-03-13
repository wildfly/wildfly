/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.jaeger;

import java.util.List;

@Deprecated
public class JaegerSpan {
    private String traceID;
    private String spanID;
    private String processID;
    private String operationName;
    private Long startTime;
    private Integer duration;
    private List<JaegerTag> tags;
    private List<JaegerLog> logs;
    private List<String> warnings;

    public String getTraceID() {
        return traceID;
    }

    public JaegerSpan setTraceID(String traceID) {
        this.traceID = traceID;
        return this;
    }

    public String getSpanID() {
        return spanID;
    }

    public JaegerSpan setSpanID(String spanID) {
        this.spanID = spanID;
        return this;
    }

    public String getOperationName() {
        return operationName;
    }

    public JaegerSpan setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    public Long getStartTime() {
        return startTime;
    }

    public JaegerSpan setStartTime(Long startTime) {
        this.startTime = startTime;
        return this;
    }

    public Integer getDuration() {
        return duration;
    }

    public JaegerSpan setDuration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public List<JaegerTag> getTags() {
        return tags;
    }

    public JaegerSpan setTags(List<JaegerTag> tags) {
        this.tags = tags;
        return this;
    }

    public List<JaegerLog> getLogs() {
        return logs;
    }

    public JaegerSpan setLogs(List<JaegerLog> logs) {
        this.logs = logs;
        return this;
    }

    @Override
    public String toString() {
        return "JaegerSpan{" +
                "traceID='" + traceID + '\'' +
                ", spanID='" + spanID + '\'' +
                ", processID='" + processID + '\'' +
                ", operationName='" + operationName + '\'' +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", tags=" + tags +
                ", logs=" + logs +
                ", warnings=" + warnings +
                '}';
    }
}
