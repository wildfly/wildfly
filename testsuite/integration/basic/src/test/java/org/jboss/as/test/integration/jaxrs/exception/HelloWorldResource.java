/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.exception;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * Simple resource + produces exception
 *
 * @author Pavel Janousek
 */
@Path("helloworld")
@Produces({"text/plain"})
public class HelloWorldResource {
    @GET
    public String getMessage() {
        return "Hello World!";
    }

    @GET
    @Path("ex1")
    @SuppressWarnings("null")
    public String getNullPointerException() {
        String a = null;
        return a.toString();
    }

    @GET
    @Path("ex2")
    public String getArrayIndexOutOfBoundsException() {
        String[] a = new String[1];
        return a[5];
    }
}
