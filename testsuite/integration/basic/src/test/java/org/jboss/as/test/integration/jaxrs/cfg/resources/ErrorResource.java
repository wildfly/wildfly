/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("error")
public class ErrorResource {

    @Inject
    private Client client;

    @Inject
    private UriInfo uriInfo;

    @POST
    public Response error(final String message) {
        final Response response = Response.serverError().entity(message).build();
        throw new WebApplicationException(response);
    }

    @POST
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    @Path("/html")
    public Response html(final String html) {
        return Response.status(Response.Status.BAD_REQUEST).entity(html).build();
    }

    /**
     * Always throws an {@code 401} with a {@code user-id} header entry and a JSON error message indicated by an
     * {@code error} key.
     *
     * @return the error response
     */
    @GET
    @Path("/auth")
    @Produces(MediaType.APPLICATION_JSON)
    public Response failedAuth() {
        final String userId = "anonymous";
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(builder.add("error", String.format("User %s is not authorized", userId)).build())
                        .header("user-id", userId)
                        .type(MediaType.APPLICATION_JSON)
                        .build()
        );
    }

    @GET
    @Path("/client/no-auth")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject noAuth() {
        try {
            // This always throws an exception, the exception should always be a WebApplicationException
            return client.target(uriInfo.getBaseUri())
                    .path("/error/auth")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    // We use the get(Class<?>) here to ensure the error is thrown
                    .get(JsonObject.class);
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();
            final JsonObjectBuilder builder;
            // If the response has a body, one was added and the setting for the
            // resteasy.original.webapplicationexception.behavior must be set to true
            if (response.hasEntity()) {
                final JsonObject json = response.readEntity(JsonObject.class);
                builder = Json.createObjectBuilder(json);
            } else {
                builder = Json.createObjectBuilder();
            }
            // Add the headers
            final JsonObjectBuilder headerBuilder = Json.createObjectBuilder();
            response.getStringHeaders().forEach((key, value) -> headerBuilder.add(key, Json.createArrayBuilder(value)));
            builder.add("headers", headerBuilder);
            return builder.build();
        }
    }

}
