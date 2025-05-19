/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry.exporter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.json.JsonObject;

/**
 * This is a simplified implementation of LogRecordData intended for test purposes only. It does not support
 * Resource or InstrumentationScopeInfo for simplicity's sake. Should those values need to be tested, this class
 * will need to be updated to support them.
 */
public class TestLogRecordData implements LogRecordData {
    private long timestampEpochNanos;
    private long observedTimestampEpochNanos;
    private SpanContext spanContext;
    private Severity severity;
    private String severityText;
    private Attributes attributes;
    private int totalAttributeCount;
    private Value<?> bodyValue;

    public TestLogRecordData() {
        this(
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            SpanContext.create("deadbeef", "deadbeef", TraceFlags.getDefault(), TraceState.getDefault()),
            Severity.INFO,
            "info",
            Attributes.empty(),
            0,
            Value.of("foo")
        );
    }

    public TestLogRecordData(long timestampEpochNanos,
                             long observedTimestampEpochNanos,
                             SpanContext spanContext,
                             Severity severity,
                             String severityText,
                             Attributes attributes,
                             int totalAttributeCount,
                             Value<?> bodyValue) {
        this.timestampEpochNanos = timestampEpochNanos;
        this.observedTimestampEpochNanos = observedTimestampEpochNanos;
        if (spanContext == null) {
            throw new NullPointerException("Null spanContext");
        } else {
            this.spanContext = spanContext;
            if (severity == null) {
                throw new NullPointerException("Null severity");
            } else {
                this.severity = severity;
                this.severityText = severityText;
                if (attributes == null) {
                    throw new NullPointerException("Null attributes");
                } else {
                    this.attributes = attributes;
                    this.totalAttributeCount = totalAttributeCount;
                    this.bodyValue = bodyValue;
                }
            }
        }
    }

    public Resource getResource() {
        return null;
    }

    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
        return null;
    }

    public long getTimestampEpochNanos() {
        return this.timestampEpochNanos;
    }

    public long getObservedTimestampEpochNanos() {
        return this.observedTimestampEpochNanos;
    }

    public SpanContext getSpanContext() {
        return this.spanContext;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public String getSeverityText() {
        return this.severityText;
    }

    public Body getBody() {
        Value<?> valueBody = getBodyValue();
        return valueBody == null
            ? Body.empty()
            : Body.string(valueBody.asString());
    }

    public Attributes getAttributes() {
        return this.attributes;
    }

    public int getTotalAttributeCount() {
        return this.totalAttributeCount;
    }

    public Value<?> getBodyValue() {
        return this.bodyValue;
    }

    public TestLogRecordData setResource(Resource resource) {
        return this;
    }

    @Override
    public String toString() {
        return "TestLogRecordData{" +
            "timestampEpochNanos=" + timestampEpochNanos +
            ", observedTimestampEpochNanos=" + observedTimestampEpochNanos +
            ", spanContext=" + spanContext +
            ", severity=" + severity +
            ", severityText='" + severityText + '\'' +
            ", attributes=" + attributes +
            ", totalAttributeCount=" + totalAttributeCount +
            ", bodyValue=" + bodyValue.asString() +
            '}';
    }

    public static TestLogRecordData fromJson(JsonObject object) {
        JsonObject spanContext = object.getJsonObject("spanContext");
        System.err.println("bodyValue: " + object.get("bodyValue"));
        return new TestLogRecordData(
            object.getInt("timestampEpochNanos"),
            object.getInt("observedTimestampEpochNanos"),
            SpanContext.create(spanContext.getString("traceId"),
                spanContext.getString("spanId"), TraceFlags.getDefault(), TraceState.getDefault()),
            Severity.valueOf(object.getString("severity")),
            object.getString("severityText"),
            Attributes.empty(),
            0,
            Value.of(object.getString("bodyValue"))
        );
    }
}
