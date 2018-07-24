package org.jboss.as.test.integration.microprofile.opentracing.application;

import org.eclipse.microprofile.opentracing.Traced;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("not-traced")
@Produces({"text/plain"})
@Traced(false)
public class NotTracedEndpoint {

    @GET
    public String get() {
        return "not-traced-called";
    }

}
