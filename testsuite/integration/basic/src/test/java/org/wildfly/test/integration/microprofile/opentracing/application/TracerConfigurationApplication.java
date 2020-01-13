package org.wildfly.test.integration.microprofile.opentracing.application;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import io.opentracing.Tracer;

/**
 * @author Sultan Zhantemirov (c) 2019 Red Hat, Inc.
 */
@ApplicationPath("tracer-config")
public class TracerConfigurationApplication extends Application {

    @Path("/get")
    public static class TestResource {

        @Inject
        private Tracer tracer;

        @GET
        @Produces("text/plain")
        public String get() {
            return tracer.toString();
        }

    }
}
