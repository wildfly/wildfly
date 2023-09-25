/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.validation;

import jakarta.ejb.Stateless;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * */

@Stateless
@Path("/")
public class TestResource {

    @POST
    @Path("put/list")
    @Consumes(MediaType.APPLICATION_JSON)
    public String putList(@NotEmpty List<String> a) {
        return a.stream().collect(Collectors.joining(", "));
    }

    @Valid
    @GET
    @Path("validate/{id}")
    public String get(@PathParam("id") @Min(value=4) int id) {
        return String.valueOf(id);
    }
}

