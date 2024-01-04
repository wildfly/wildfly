/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.spec.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * User: rsearls
 * Date: 2/2/17
 */
@Path("exampleTwo")
public class JaxrsAppResourceTwo {
    @GET
    @Produces("text/plain")
    public String get() {
        return "Two Hello world!";
    }
}
