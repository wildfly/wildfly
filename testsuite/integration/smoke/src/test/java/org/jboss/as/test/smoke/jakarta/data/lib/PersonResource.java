/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/** Provides a REST API for manipulating Person objects. */
@Path("/person")
public class PersonResource {

    @Inject
    private Recruiter recruiter;

    @Path("{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Person get(@PathParam("name") String name) {
        Optional<Person> optional = recruiter.find(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException();
    }

    @Path("{name}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Person recruit(@PathParam("name") String name, @QueryParam("birthday") String birthday) {
        return recruiter.recruit(name, LocalDate.parse(birthday));
    }
}
