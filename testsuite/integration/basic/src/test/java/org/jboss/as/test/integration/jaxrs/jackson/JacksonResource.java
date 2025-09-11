/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jackson;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Stuart Douglas
 */
@Path("/jackson")
public class JacksonResource {

    @GET
    @Produces("application/vnd.customer+json")
    public Customer get() {
        return new Customer("John", "Citizen");
    }


    @Path("/duration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Duration duration() {
        return Duration.of(1, ChronoUnit.SECONDS);
    }


    @Path("/optional")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Optional<String> optional() {
        return Optional.of("optional string");
    }

    @GET
    @Path("named")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public NamedEntity named() {
        return new NamedEntity(1L, "Jackson");
    }
}
