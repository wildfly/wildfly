/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.subresource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class PersonResource {

    private String name;

    public PersonResource(String name) {
        this.name = name;
    }

    @GET
    public String getName() {
        return name;
    }

    @GET
    @Path("/address")
    public String getClassName() {
        return name + "'s address is unknown.";
    }
}
