/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.http;

import static org.jboss.as.test.shared.observability.collector.CollectorUtil.fromSpan;

import java.io.IOException;
import java.util.function.Consumer;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.as.test.shared.observability.signals.trace.SimpleSpan;

public class TracesServlet extends OtlpHttpServlet {
    private final Consumer<SimpleSpan> traceConsumer;

    public TracesServlet(Consumer<SimpleSpan> traceConsumer) {
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
        traceRequest.getResourceSpansList().forEach(rs ->
                rs.getScopeSpansList().forEach(ss ->
                        ss.getSpansList().forEach(s ->
                                fromSpan(traceConsumer, rs.getResource(), s))));
    }
}
