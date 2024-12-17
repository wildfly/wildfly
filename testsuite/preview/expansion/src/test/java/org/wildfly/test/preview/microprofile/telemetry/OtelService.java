/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.microprofile.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@RequestScoped
@Path("/otel")
public class OtelService {
    @Inject
    private Tracer tracer;

    @GET
    public String sayHello(@QueryParam("name") String name) {
        final Span span = tracer.spanBuilder("Saying hello from server1").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.addEvent("some-event");
            span.setAttribute("name", name);
            span.end();
        }

        return "Hello, " + name;
    }

}
