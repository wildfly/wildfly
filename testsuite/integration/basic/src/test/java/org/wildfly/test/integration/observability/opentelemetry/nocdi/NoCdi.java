package org.wildfly.test.integration.observability.opentelemetry.nocdi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/")
public class NoCdi {

    @GET
    @Path("/json")
    @Produces({"application/json"})
    public String getJson() {
        return "{\"result\":\"Hello World\"}";
    }
}
