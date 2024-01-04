/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.packaging.ear;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("helloworld")
@Produces({"text/plain"})
@Stateless
public class HelloWorldResource {

    private String message;

    @PostConstruct
    public void postConstruct() {

        message = "Hello World!";
    }

    @GET
    public String getMessage() {
        return message;
    }
}
