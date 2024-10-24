/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("precedence")
public class PreconditionRfc7232PrecedenceResource {

    @Inject
    private Request request;

    @GET
    public Response get() {
        final ZonedDateTime lastModified = ZonedDateTime.of(2007, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));
        Response.ResponseBuilder rb = request.evaluatePreconditions(Date.from(lastModified.toInstant()), new EntityTag("1"));
        if (rb != null) {
            return rb.entity("preconditions failed").build();
        }
        return Response.ok("preconditions met", MediaType.TEXT_PLAIN_TYPE).build();
    }
}
