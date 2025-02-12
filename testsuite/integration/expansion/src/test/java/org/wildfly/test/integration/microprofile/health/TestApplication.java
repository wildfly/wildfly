/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@ApplicationPath("/microprofile")
public class TestApplication extends Application {

    @Path("/myApp")
    public static class Resource {

        @POST
        @Produces("text/plain")
        public Response changeProbeOutcome(@FormParam("up") boolean up) {
            MyLiveProbe.up = up;
            return Response.ok().build();
        }
    }

}
