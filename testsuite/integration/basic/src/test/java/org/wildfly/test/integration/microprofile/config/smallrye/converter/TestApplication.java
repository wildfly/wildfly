/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@ApplicationPath("/custom-converter")
public class TestApplication extends Application {

    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = "int_converted_to_102_by_priority_of_custom_converter", defaultValue = "42")
        int convertedTo102;

        @Inject
        @ConfigProperty(name = "string_converted_by_priority_of_custom_converter", defaultValue = "I should not be here")
        String convertedString;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder sb = new StringBuilder();
            sb.append("int_converted_to_102_by_priority_of_custom_converter = " + convertedTo102 + "\n");
            sb.append("string_converted_by_priority_of_custom_converter = " + convertedString + "\n");
            return Response.ok(sb).build();
        }
    }
}
