/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient.deployment.resource;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.wildfly.test.integration.microprofile.restclient.deployment.model.Message;

/**
 * This is a simple endpoint to invoke injected client calls.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/client")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class ClientMessageResource {

    @Inject
    @RestClient
    private MessageClient messageClient;

    @GET
    public List<Message> messages() {
        return messageClient.messages();
    }

    @GET
    @Path("{id}")
    public Message getUser(@PathParam("id") final int id) {
        return messageClient.get(id);
    }

    @POST
    public Response push(final Message user) {
        return messageClient.push(user);
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") final int id) {
        return messageClient.delete(id);
    }
}
