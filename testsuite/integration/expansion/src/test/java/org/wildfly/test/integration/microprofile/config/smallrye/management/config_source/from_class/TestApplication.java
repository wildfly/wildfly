/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_class;

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

    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = CustomConfigSource.PROP_NAME)
        String prop;

        @Inject
        @ConfigProperty(name = CustomConfigSourceServiceLoader.PROP_NAME)
        String propFromServiceLoader;

        @Inject
        @ConfigProperty(name = CustomConfigSource.PROP_NAME_OVERRIDEN_BY_SERVICE_LOADER)
        String propOverridenByServiceLoader;

        @Inject
        @ConfigProperty(name = CustomConfigSourceAServiceLoader.PROP_NAME_SAME_ORDINALITY_OVERRIDE)
        String propSameOrdinalityOverridenByFqcn;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();
            text.append(CustomConfigSource.PROP_NAME + " = " + prop + "\n");
            text.append(CustomConfigSourceServiceLoader.PROP_NAME + " = " + propFromServiceLoader + "\n");
            text.append(CustomConfigSource.PROP_NAME_OVERRIDEN_BY_SERVICE_LOADER + " = " + propOverridenByServiceLoader + "\n");
            text.append(CustomConfigSourceAServiceLoader.PROP_NAME_SAME_ORDINALITY_OVERRIDE + " = " +
                    propSameOrdinalityOverridenByFqcn + "\n");
            return Response.ok(text).build();
        }
    }
}
