/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jackson;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author Stuart Douglas
 */
@Path("/country")
public class JacksonCountryResource {

    @GET
    @Produces("application/vnd.customer+json")
    public Country get() {
        return new Country(3, "Australia", "Hot");
    }
}
