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

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_root_dir;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author Kabir Khan
 */
@ApplicationPath("/custom-config-source")
public class TestApplication extends Application {
    static final String FROM_A1 = "from-a1";
    static final String FROM_A2 = "from-a2";
    static final String FROM_B = "from-b";
    static final String X_D_OVERRIDES_A = "x-d-overrides-a";
    static final String Y_A_OVERRIDES_B = "y-a-overrides-b";
    static final String Z_C_OVERRIDES_A = "y-c-overrides-a";

    static final String NOT_AVAILABLE_NESTED_DIR_UNDER_A = "not-available-a";
    static final String NOT_AVAILABLE_ROOT_FILE = "not-available-root-file";
    static final String DEFAULT = "default";


    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = FROM_A1)
        String fromA1;

        @Inject
        @ConfigProperty(name = FROM_A2)
        String fromA2;

        @Inject
        @ConfigProperty(name = FROM_B)
        String fromB;

        @Inject
        @ConfigProperty(name = X_D_OVERRIDES_A)
        String xDOverridesA;

        @Inject
        @ConfigProperty(name = Y_A_OVERRIDES_B)
        String yAOverridesB;

        @Inject
        @ConfigProperty(name = Z_C_OVERRIDES_A)
        String zCOverridesA;

        @Inject
        @ConfigProperty(name = NOT_AVAILABLE_NESTED_DIR_UNDER_A, defaultValue = DEFAULT)
        String notAvailableA;

        @Inject
        @ConfigProperty(name = NOT_AVAILABLE_ROOT_FILE, defaultValue = DEFAULT)
        String notAvailableRootFile;



        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();
            text.append(FROM_A1 + " = " + fromA1 + "\n");
            text.append(FROM_A2 + " = " + fromA2 + "\n");
            text.append(FROM_B + " = " + fromB + "\n");
            text.append(X_D_OVERRIDES_A + " = " + xDOverridesA + "\n");
            text.append(Y_A_OVERRIDES_B + " = " + yAOverridesB + "\n");
            text.append(Z_C_OVERRIDES_A + " = " + zCOverridesA + "\n");
            text.append(NOT_AVAILABLE_NESTED_DIR_UNDER_A + " = " + notAvailableA + "\n");
            text.append(NOT_AVAILABLE_ROOT_FILE + " = " + notAvailableRootFile + "\n");

            return Response.ok(text).build();
        }
    }
}
