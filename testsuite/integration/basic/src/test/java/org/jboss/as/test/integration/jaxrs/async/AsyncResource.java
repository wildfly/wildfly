/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.async;

import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author Stuart Douglas
 */
@Path("/async")
public class AsyncResource {
    @GET
    @Path("basic")
    @Produces("text/plain")
    public void getBasic(@Suspended final AsyncResponse response) throws Exception {
        response.setTimeout(1, TimeUnit.SECONDS);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    Response jaxrs = Response.ok("basic").type(MediaType.TEXT_PLAIN).build();
                    response.resume(jaxrs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
}
