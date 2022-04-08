/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

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
