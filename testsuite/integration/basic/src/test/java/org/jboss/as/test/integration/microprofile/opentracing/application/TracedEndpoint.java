package org.jboss.as.test.integration.microprofile.opentracing.application;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("traced")
@Produces({"text/plain"})
public class TracedEndpoint {

    @GET
    public String get() {
        return "traced-called";
    }

}
