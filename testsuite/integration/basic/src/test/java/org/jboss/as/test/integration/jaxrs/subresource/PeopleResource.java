/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.subresource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("person")
public class PeopleResource {

    @Path("{name}")
    public PersonResource findPerson(@PathParam("name") String name) {
        return new PersonResource(name);
    }
}
