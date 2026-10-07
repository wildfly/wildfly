/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel.application;

import java.util.List;

import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TestReactiveMessagingOtelEndpoint {

    @Inject
    TestReactiveMessagingOtelBean testReactiveMessagingOtelBean;

    @POST
    @Path("/")
    public Response publish(@FormParam("value") String value,
                           @FormParam("testName") String testName,
                           @FormParam("iteration") Integer iteration) {
        // Add custom span attributes for test isolation and tracking
        Span span = Span.current();
        if (testName != null) {
            span.setAttribute("test.name", testName);
        }
        if (iteration != null) {
            span.setAttribute("test.iteration", iteration);
        }

        // Pass test metadata to send() so it can be propagated through Kafka message
        testReactiveMessagingOtelBean.send(value, testName, iteration);
        return Response.ok().build();
    }

    @GET
    @Path("/")
    public List<String> readMessages() {
        return testReactiveMessagingOtelBean.getReceived();
    }
}
