/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.grpc;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.debugLog;
import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromMetric;

import java.util.function.Consumer;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import org.jboss.as.test.shared.observability.signals.SimpleMetric;

public class MetricsServiceImpl extends MetricsServiceGrpc.MetricsServiceImplBase {

    private final Consumer<SimpleMetric> metricsReceived;

    public MetricsServiceImpl(Consumer<SimpleMetric> metricsReceived) {
        this.metricsReceived = metricsReceived;
    }

    @Override
    public void export(ExportMetricsServiceRequest request,
                       StreamObserver<ExportMetricsServiceResponse> responseObserver) {
        try {
            debugLog("Metrics request received");
            request.getResourceMetricsList().forEach(rm ->
                rm.getScopeMetricsList().forEach(sm ->
                    sm.getMetricsList().forEach(m -> fromMetric(metricsReceived, rm.getResource(), sm.getScope(), m))));

            ExportMetricsServiceResponse response = ExportMetricsServiceResponse.newBuilder()
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
