/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.vertx;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/echo")
public class ServiceEndpoint {

    @Inject
    private EchoService echoService;

    @GET
    @Path("{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sayHi(@PathParam("name") String name) {
        try {
            String message = echoService.echo(name).get();
            return Response.ok(message).build();
        } catch (Exception e) {
            return Response.status(500).build();
        }
    }

    @GET
    @Path("/fail/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sayHiFail(@PathParam("name") String name) {
        try {
            String message = echoService.echoFail(name).get();
            return Response.ok(message).build();
        } catch (Exception e) {
            return Response.status(500).entity(e).build();
        }
    }

}
