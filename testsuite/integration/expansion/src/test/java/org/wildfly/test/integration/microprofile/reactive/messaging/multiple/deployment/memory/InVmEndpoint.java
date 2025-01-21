/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.multiple.deployment.memory;

import java.util.List;

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
public class InVmEndpoint {

    @Inject
    InVmMessagingBean inVmMessagingBean;

    @POST
    @Path("/")
    public Response publish(@FormParam("value") String value) {
        inVmMessagingBean.send(value);
        return Response.ok().build();
    }

    @GET
    @Path("/")
    public List<String> readMessages() {
        return inVmMessagingBean.getReceived();
    }

}
