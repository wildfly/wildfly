/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.http;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromByteString;
import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromKeyValueList;

import java.io.IOException;
import java.util.function.Consumer;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.test.shared.observability.signals.trace.Span;

public class TracesServlet extends OtlpHttpServlet {
    private final Consumer<Span> traceConsumer;

    public TracesServlet(Consumer<Span> traceConsumer) {
        this.traceConsumer = traceConsumer;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            processTraces(ExportTraceServiceRequest.parseFrom(readAllBytes(req.getInputStream())));
            setSuccessResponse(resp, ExportTraceServiceResponse.getDefaultInstance().toByteArray());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void processTraces(ExportTraceServiceRequest traceRequest) {
        for (ResourceSpans rs : traceRequest.getResourceSpansList()) {
            for (ScopeSpans ss : rs.getScopeSpansList()) {
                ss.getSpansList().forEach(s -> traceConsumer.accept(
                        Span.builder()
                                .traceId(fromByteString(s.getTraceId()))
                                .spanId(fromByteString(s.getSpanId()))
                                .name(s.getName())
                                .kind(s.getKindValue())
                                .traceState(s.getTraceState())
                                .parentSpanId(fromByteString(s.getParentSpanId()))
                                .flags(s.getFlags())
                                .startTimeUnixNano(s.getStartTimeUnixNano())
                                .endTimeUnixNano(s.getEndTimeUnixNano())
                                .attributes(fromKeyValueList(s.getAttributesList()))
                                .droppedAttributesCount(s.getDroppedAttributesCount())
                                .events(s.getEventsList())
                                .droppedEventsCount(s.getDroppedEventsCount())
                                .links(s.getLinksList())
                                .droppedLinksCount(s.getDroppedLinksCount())
                                .status(s.getStatus()).build()
                ));
            }
        }
    }
}
