/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("user")
@ApplicationScoped
public class UserResource {
    private final Map<Long, User> users = new ConcurrentHashMap<>();

    @Inject
    private UriInfo uriInfo;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/echo")
    public User echoUser(final User echo) {
        return echo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public User user(@PathParam("id") long id) {
        final User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("User " + id + " not found");
        }
        return user;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(final User user) {
        if (users.put(user.getId(), user) == null) {
            return Response.created(uriInfo.getBaseUriBuilder().path("user/" + user.getId()).build()).build();
        }
        throw new WebApplicationException("User " + user.getId() + " already registered");
    }

    @PATCH
    @Path("{id}")
    @Consumes("application/merge-patch+json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mergePatchUser(@PathParam("id") final long id,  final User user) {
        final User found = users.get(id);
        if (id != user.getId()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(user).build();
        }
        if (found == null) {
            throw new NotFoundException("User " + id + " not found");
        }
        users.put(id, user);
        return Response.noContent().location(uriInfo.getBaseUriBuilder().path("user/" + id).build()).build();
    }
}
