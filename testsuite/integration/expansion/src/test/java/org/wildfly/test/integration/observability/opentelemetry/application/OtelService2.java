/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry.application;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/")
public class OtelService2 {
    @Inject
    private Tracer tracer;

    @Context
    private HttpHeaders headers;

    @GET
    @Path("contextProp2")
    public Response contextProp2() {
        final Span span = tracer.spanBuilder("Handling contextProp2 Request").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("method_called", "contextProp2");
            span.addEvent("The method contextProp2 was called");

            String traceParent = headers.getHeaderString("traceparent");
            if (traceParent == null || traceParent.isEmpty()) {
                throw new WebApplicationException("Missing traceparent header");
            }
            span.setAttribute("traceParent", traceParent);
            span.end();
        }

        return Response.noContent().build();
    }
}
