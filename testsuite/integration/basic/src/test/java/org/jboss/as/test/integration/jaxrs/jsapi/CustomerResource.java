/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jsapi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author Pavel Janousek
 */
@Path("jsapi")
public class CustomerResource {

    @GET
    @Produces({"application/xml"})
    public Customer get() {
        return new Customer("John", "Citizen");
    }
}
