/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.validation;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EchoResource {
    @POST
    @Path("echoThroughAbstract")
    Response validateEchoThroughAbstractClass(@Valid DummySubclass payload);

    @POST
    @Path("echo")
    Response validateEchoThroughClass(@Valid DummyClass payload);
}
