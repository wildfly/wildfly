/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.test.integration.microprofile.faulttolerance.opentracing.application;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import java.util.stream.Collectors;
import javax.ws.rs.Produces;
import org.eclipse.microprofile.opentracing.Traced;

@Path("/tracer")
@Traced(false)
public class TracerIdentity {

    @Inject
    private Tracer tracer;

    @GET
    @Path("/spans")
    @Produces("text/plain")
    public String get() {
        MockTracer jTracer = (MockTracer) tracer;
        return jTracer.finishedSpans().stream()
                .map(span -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append("{\"parentId\":").append('"').append(span.parentId()).append('"');
                    builder.append(",\"traceId\":").append('"').append(span.context().traceId()).append('"');
                    builder.append(",\"spanId\":").append('"').append(span.context().spanId()).append('"');
                    builder.append(",\"operationName\":").append('"').append(span.operationName()).append('"');
                    if (span.logEntries() != null && !span.logEntries().isEmpty()) {
                        builder.append(",\"logs\":").append("{\"event\":").append('"').append(span.logEntries().get(0).fields().get("event").toString()).append('"').append("}");
                    }
                    builder.append("}");
                    return builder.toString();
                })
                .collect(Collectors.joining(";"));
    }

    @GET
    @Path("/reset")
    @Produces("text/plain")
    public String resetTracer() {
        MockTracer jTracer = (MockTracer) tracer;
        jTracer.reset();
        return "OK";
    }
}
