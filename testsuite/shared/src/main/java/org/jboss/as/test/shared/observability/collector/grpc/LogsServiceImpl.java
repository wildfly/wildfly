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
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import org.jboss.as.test.shared.observability.signals.logs.LogEntry;

public class LogsServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {

    private final Consumer<LogEntry> logConsumer;

    public LogsServiceImpl(Consumer<LogEntry> logConsumer) {
        this.logConsumer = logConsumer;
    }

    @Override
    public void export(ExportLogsServiceRequest request,
                       StreamObserver<ExportLogsServiceResponse> responseObserver) {
        try {
            debugLog("Logs request received");
            var list = request.getResourceLogsList();
            list.forEach(rl -> {
                rl.getScopeLogsList().forEach(sl -> {
                    sl.getLogRecordsList().forEach(lr -> {
                        logConsumer.accept(new LogEntry(
                                fromByteString(lr.getTraceId()),
                                fromByteString(lr.getSpanId()),
                                lr.getBody().getStringValue(),
                                lr.getTimeUnixNano(),
                                lr.getObservedTimeUnixNano(),
                                lr.getSeverityNumberValue(),
                                lr.getSeverityText(),
                                fromKeyValueList(lr.getAttributesList()),
                                lr.getFlags()));
                    });
                });
            });
            ExportLogsServiceResponse response = ExportLogsServiceResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
