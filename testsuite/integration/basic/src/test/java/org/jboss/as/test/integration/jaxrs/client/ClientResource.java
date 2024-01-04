/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author <a href="mailto:kanovotn@redhat.com">Katerina Novotna</a>
 */
@Path("/client")
public class ClientResource {

    @GET
    @Produces("text/plain")
    public String get() {
        return "GET: Hello World!";
    }

    @POST
    @Consumes("text/plain")
    public String post(String str) {
        return "POST: " + str;
    }

    @PUT
    @Consumes("text/plain")
    public String put(String str) {
        return "PUT: " + str;
    }

    @DELETE
    @Produces("text/plain")
    public String delete() {
        return "DELETE:";
    }


}
