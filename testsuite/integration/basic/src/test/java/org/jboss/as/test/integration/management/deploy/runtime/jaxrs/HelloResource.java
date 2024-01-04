/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.jaxrs;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.ejb.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014
 * Red Hat, inc.
 */
@Path("/")
@Singleton
public class HelloResource {

    private AtomicReference<String> message = new AtomicReference<>("World");

    private String createHelloMessage(final String msg) {
        return "Hello " + msg + "!";
    }

    @GET
    @Path("/")
    @Produces({"text/plain"})
    public String getHelloWorld() {
        return createHelloMessage(message.get());
    }

    @GET
    @Path("/json")
    @Produces({"application/json"})
    public JsonObject getHelloWorldJSON() {
        return Json.createObjectBuilder()
                .add("result", createHelloMessage(message.get()))
                .build();
    }

    @GET
    @Path("/xml")
    @Produces({"application/xml"})
    public String getHelloWorldXML() {
        return "<xml><result>" + createHelloMessage(message.get()) + "</result></xml>";
    }

    @PUT
    @Consumes("text/plain")
    @Path("/update")
    public void updateMessage(@QueryParam("content") @DefaultValue("Hello") String content) {
        message.set(content);
    }

    @Path("/sub")
    public SubHelloResource sub() {
        return new SubHelloResource();
    }
}
