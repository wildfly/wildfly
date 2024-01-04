/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry.application;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class OtelService {
    @Inject
    private Tracer tracer;

    @Context
    UriInfo uriInfo;

    @GET
    public Response endpoint() {
        final Span span = tracer.spanBuilder("Custom span").startSpan();

        span.makeCurrent();
        span.addEvent("Custom event");
        span.end();

        return Response.noContent().build();
    }

    @Context
    private HttpHeaders headers;

    @GET
    @Path("contextProp1")
    public Response contextProp1() {
        final Span span = tracer.spanBuilder("Making second request").startSpan();
        span.makeCurrent();

        try (Client client = ClientBuilder.newClient()) {
            client.target(uriInfo.getBaseUriBuilder().path("contextProp2"))
                    .request()
                    .get();
        }
        span.end();

        return Response.noContent().build();
    }

    @GET
    @Path("contextProp2")
    public Response contextProp2() {
        final Span span = tracer.spanBuilder("Recording traceparent").startSpan();
        span.makeCurrent();
        String traceParent = headers.getHeaderString("traceparent");
        if (traceParent == null || traceParent.isEmpty()) {
            throw new WebApplicationException("Missing traceparent header");
        }
        span.addEvent("Test event",
                Attributes.builder()
                        .put("traceparent", traceParent)
                        .build());
        span.end();

        return Response.noContent().build();
    }
}
