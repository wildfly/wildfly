/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
