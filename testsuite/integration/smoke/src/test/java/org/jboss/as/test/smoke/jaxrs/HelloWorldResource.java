/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/helloworld")
@Produces({"text/plain"})
public class HelloWorldResource {
    @GET
    public String getMessage() {
        return "Hello World!";
    }
}
