/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel.application;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TestReactiveMessagingOtelEndpoint {

    @Inject
    TestReactiveMessagingOtelBean testReactiveMessagingOtelBean;

    @Inject
    Config config;

    @POST
    @Path("/")
    public Response publish(@FormParam("value") String value) {
        testReactiveMessagingOtelBean.send(value);
        return Response.ok().build();
    }

    @GET
    @Path("/")
    public List<String> readMessages() {
        return testReactiveMessagingOtelBean.getReceived();
    }

    @DELETE
    @Path("/")
    public void deleteStoredMessages() {
        testReactiveMessagingOtelBean.clear();
    }

    @GET
    @Path("/property")
    public boolean getConfigProperty(@QueryParam("prop") String property) {
        boolean value = config.getValue(property, Boolean.class);
        return value;
    }

}
