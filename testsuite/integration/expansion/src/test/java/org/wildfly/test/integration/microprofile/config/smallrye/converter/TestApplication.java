/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

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
        MyString convertedString;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder sb = new StringBuilder();
            sb.append("int_converted_to_102_by_priority_of_custom_converter = " + convertedTo102 + "\n");
            sb.append("string_converted_by_priority_of_custom_converter = " + convertedString.value + "\n");
            return Response.ok(sb).build();
        }
    }
}
