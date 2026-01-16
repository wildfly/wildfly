/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.grpc;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.debugLog;
import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromSpan;

import java.util.function.Consumer;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import org.jboss.as.test.shared.observability.signals.trace.SimpleSpan;

public class TraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

    private final Consumer<SimpleSpan> traceConsumer;

    public TraceServiceImpl(Consumer<SimpleSpan> traceConsumer) {
        this.traceConsumer = traceConsumer;
    }

    @Override
    public void export(ExportTraceServiceRequest request,
                       StreamObserver<ExportTraceServiceResponse> responseObserver) {
        try {
            debugLog("Trace request received");
            request.getResourceSpansList().forEach(rs ->
                    rs.getScopeSpansList().forEach(ss -> {
                        ss.getSpansList().forEach(s ->
                                fromSpan(traceConsumer, rs.getResource(), s));
                    }));

            ExportTraceServiceResponse response = ExportTraceServiceResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
