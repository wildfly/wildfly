/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@ApplicationPath("/custom-config-source")
public class TestApplication extends Application {
    static final String FROM_A = "from-a";
    static final String FROM_B = "from-b";
    static final String B_OVERRIDES_A = "b-overrides-a";


    static final String NOT_AVAILABLE_NESTED_DIR_UNDER_A = "not-available-a";
    static final String NOT_AVAILABLE_NESTED_DIR_UNDER_B = "not-available-b";
    static final String DEFAULT = "default";

    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = FROM_A)
        String fromA;

        @Inject
        @ConfigProperty(name = FROM_B)
        String fromB;

        @Inject
        @ConfigProperty(name = B_OVERRIDES_A)
        String bOverridesA;

        @Inject
        @ConfigProperty(name = NOT_AVAILABLE_NESTED_DIR_UNDER_A, defaultValue = DEFAULT)
        String notAvailableA;

        @Inject
        @ConfigProperty(name = NOT_AVAILABLE_NESTED_DIR_UNDER_B, defaultValue = DEFAULT)
        String notAvailableB;


        @GET
        @Produces("text/plain")
        public Response doGet() {

            StringBuilder text = new StringBuilder();
            text.append(FROM_A + " = " + fromA + "\n");
            text.append(FROM_B + " = " + fromB + "\n");
            text.append(B_OVERRIDES_A + " = " + bOverridesA + "\n");
            text.append(NOT_AVAILABLE_NESTED_DIR_UNDER_A + " = " + notAvailableA + "\n");
            text.append(NOT_AVAILABLE_NESTED_DIR_UNDER_B + " = " + notAvailableB + "\n");
            return Response.ok(text).build();
        }
    }
}
