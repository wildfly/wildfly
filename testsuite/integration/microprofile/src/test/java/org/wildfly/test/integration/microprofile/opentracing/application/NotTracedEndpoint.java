package org.wildfly.test.integration.microprofile.opentracing.application;

import org.eclipse.microprofile.opentracing.Traced;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("not-traced")
@Produces({"text/plain"})
@Traced(false)
public class NotTracedEndpoint {

    @GET
    public String get() {
        return "not-traced-called";
    }

}
