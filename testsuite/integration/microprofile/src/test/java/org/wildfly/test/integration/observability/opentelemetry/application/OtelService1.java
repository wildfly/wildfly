/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry.application;

import java.net.URI;
import java.net.URISyntaxException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@RequestScoped
@Path("/")
public class OtelService1 {
    @Context
    private UriInfo uriInfo;

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

    @GET
    @Path("contextProp1")
    public Response contextProp1() throws URISyntaxException {
        final Span span = tracer.spanBuilder("Handling contextProp1 request from server1").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("method_called", "contextProp1");
            span.addEvent("The method contextProp1 was called");

            span.end();
            try (Client client = ClientBuilder.newClient()) {
                URI baseUri = uriInfo.getBaseUri();
                URI targetUri = new URI(baseUri.getScheme() +"://" +
                        baseUri.getHost() + ":" +
                        baseUri.getPort() + "/service2/contextProp2");

                Response response = client.target(targetUri)
                        .request()
                        .get();

                if (response.getStatus() != 204) {
                    throw new WebApplicationException("Second server not found.");
                }

            }
        }


        return Response.noContent().build();
    }
}
