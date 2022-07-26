package org.wildfly.test.integration.microprofile.opentracing.application;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("traced")
@Produces({"text/plain"})
public class TracedEndpoint {

    @GET
    public String get() {
        return "traced-called";
    }

}
