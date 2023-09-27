/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.atom;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author Stuart Douglas
 */
@Path("/atom")
public class AtomResource {

    @GET
    @Produces("application/atom+xml")
    public Customer get() {
        return new Customer("John", "Citizen");
    }
}
