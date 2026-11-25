/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Paul Ferraro
 */
@Path("/echo")
@Produces(MediaType.TEXT_PLAIN)
public class EchoResource {

    @GET
    @Path("{value}")
    public String echo(@PathParam("value") String value) {
        return value;
    }
}
