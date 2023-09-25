/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.metrics.application;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/hello")
public class TestResource {
    @GET
    @Produces("text/plain")
    public Response hello() {
        return Response.ok("Hello From WildFly!").build();
    }
}
