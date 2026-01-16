/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.http;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromMetric;

import java.io.IOException;
import java.util.function.Consumer;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.test.shared.observability.signals.SimpleMetric;

public class MetricsServlet extends OtlpHttpServlet {
    protected Consumer<SimpleMetric> metricsConsumer;

    public MetricsServlet(Consumer<SimpleMetric> metricsConsumer) {
        this.metricsConsumer = metricsConsumer;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            var metrics = ExportMetricsServiceRequest.parseFrom(readAllBytes(req.getInputStream()));
            processMetrics(metrics);
            setSuccessResponse(resp, ExportTraceServiceResponse.getDefaultInstance().toByteArray());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void processMetrics(ExportMetricsServiceRequest metricsRequest) {
        metricsRequest.getResourceMetricsList().forEach(rm ->
                rm.getScopeMetricsList().forEach(sm ->
                        sm.getMetricsList()
                                .forEach(m ->
                                        fromMetric(metricsConsumer, rm.getResource(), sm.getScope(), m))));
    }
}
