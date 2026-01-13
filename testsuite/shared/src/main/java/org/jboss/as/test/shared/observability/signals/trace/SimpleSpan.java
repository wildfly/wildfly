/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals.trace;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromKeyValueList;

import java.util.List;
import java.util.Map;

public record SimpleSpan(String traceId,
                         String spanId,
                         String traceState,
                         String parentSpanId,
                         int flags,
                         String name,
                         int kind,
                         long startTimeUnixNano,
                         long endTimeUnixNano,
                         Map<String, String> attributes,
                         Map<String, String> resourceAttributes,
                         List<SimpleEvent> events) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String traceId;
        private String spanId;
        private String traceState;
        private String parentSpanId;
        private int flags;
        private String name;
        private int kind;
        private long startTimeUnixNano;
        private long endTimeUnixNano;
        private Map<String, String> attributes;
        private Map<String, String> resourceAttributes;
        private List<SimpleEvent> events;

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder traceState(String traceState) {
            this.traceState = traceState;
            return this;
        }

        public Builder parentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        public Builder flags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder kind(int kind) {
            this.kind = kind;
            return this;
        }

        public Builder startTimeUnixNano(long startTimeUnixNano) {
            this.startTimeUnixNano = startTimeUnixNano;
            return this;
        }

        public Builder endTimeUnixNano(long endTimeUnixNano) {
            this.endTimeUnixNano = endTimeUnixNano;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder resourceAttributes(Map<String, String> resourceAttributes) {
            this.resourceAttributes = resourceAttributes;
            return this;
        }

        public Builder events(List<io.opentelemetry.proto.trace.v1.Span.Event> events) {
            this.events = events.stream().map(e ->
                            new SimpleEvent(e.getName(),
                                    e.getTimeUnixNano(),
                                    fromKeyValueList(e.getAttributesList())))
                    .toList();
            return this;
        }

        public SimpleSpan build() {
            return new SimpleSpan(traceId, spanId, traceState, parentSpanId, flags, name, kind,
                    startTimeUnixNano, endTimeUnixNano, attributes, resourceAttributes, events);
        }
    }
}
