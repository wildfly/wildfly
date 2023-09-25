/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi.service.multimodule;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * @author Joachim Grimm
 */
@Path("/echo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResource {

    @Inject
    private TestEjb testEjb;

    @POST
    @Operation(summary = "Test the indexing of multi module deployments")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TestResponse.class)),
            description = "Indexes found")
    public TestResponse echo(TestRequest request) {
        return testEjb.hello(request);
    }
}
