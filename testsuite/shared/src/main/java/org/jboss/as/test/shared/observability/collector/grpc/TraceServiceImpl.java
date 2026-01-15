/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.grpc;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.debugLog;
import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromByteString;
import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromKeyValueList;

import java.util.function.Consumer;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import org.jboss.as.test.shared.observability.signals.trace.Span;

public class TraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

    private final Consumer<Span> traceConsumer;

    public TraceServiceImpl(Consumer<Span> traceConsumer) {
        this.traceConsumer = traceConsumer;
    }

    @Override
    public void export(ExportTraceServiceRequest request,
                       StreamObserver<ExportTraceServiceResponse> responseObserver) {
        try {
            debugLog("Trace request received");
            var list = request.getResourceSpansList();
            list.forEach(rs -> {
                rs.getScopeSpansList().forEach(ss -> {
                    ss.getSpansList().forEach(s -> {
                        traceConsumer.accept(Span.builder()
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
                                .status(s.getStatus()).build());
                    });
                });
            });

            ExportTraceServiceResponse response = ExportTraceServiceResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
