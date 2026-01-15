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

import io.opentelemetry.proto.trace.v1.Status;

public record Span(String traceId, String spanId, String traceState, String parentSpanId, int flags, String name,
                   int kind, long startTimeUnixNano, long endTimeUnixNano, Map<String, String> attributes,
                   int droppedAttributesCount, List<Event> events, int droppedEventsCount, List<Link> links,
                   int droppedLinksCount, TraceStatus status) {

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
        private int droppedAttributesCount;
        private List<Event> events;
        private int droppedEventsCount;
        private List<Link> links;
        private int droppedLinksCount;
        private TraceStatus status;

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

        public Builder droppedAttributesCount(int droppedAttributesCount) {
            this.droppedAttributesCount = droppedAttributesCount;
            return this;
        }

        public Builder events(List<io.opentelemetry.proto.trace.v1.Span.Event> events) {
            this.events = events.stream().map(e ->
                    new Event(e.getName(),
                            e.getTimeUnixNano(),
                            fromKeyValueList(e.getAttributesList()),
                            e.getDroppedAttributesCount())).toList();
            return this;
        }

        public Builder droppedEventsCount(int droppedEventsCount) {
            this.droppedEventsCount = droppedEventsCount;
            return this;
        }

        public Builder links(List<io.opentelemetry.proto.trace.v1.Span.Link> links) {
            this.links = links.stream().map(l ->
                            new Link(l.getTraceId().toString(),
                                    l.getSpanId().toString(),
                                    l.getTraceState(),
                                    fromKeyValueList(l.getAttributesList()),
                                    l.getDroppedAttributesCount(),
                                    l.getDroppedAttributesCount()))
                    .toList();
            return this;
        }

        public Builder droppedLinksCount(int droppedLinksCount) {
            this.droppedLinksCount = droppedLinksCount;
            return this;
        }

        public Builder status(Status status) {
            this.status = new TraceStatus(status.getMessage(),
                    status.getCode().getNumber());
            return this;
        }

        public Span build() {
            return new Span(traceId, spanId, traceState, parentSpanId, flags, name, kind,
                    startTimeUnixNano, endTimeUnixNano, attributes, droppedAttributesCount,
                    events, droppedEventsCount, links, droppedLinksCount, status);
        }
    }


}
