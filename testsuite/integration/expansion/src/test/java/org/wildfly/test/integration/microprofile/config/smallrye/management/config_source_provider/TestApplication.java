/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source_provider;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_class.CustomConfigSource;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@ApplicationPath("/custom-config-source-provider")
public class TestApplication extends Application {

    @Path("/test")
    public static class Resource {
        @Inject
        @ConfigProperty(name = CustomConfigSource.PROP_NAME)
        String prop;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();
            text.append(CustomConfigSource.PROP_NAME + " = " + prop + "\n");
            return Response.ok(text).build();
        }
    }
}
