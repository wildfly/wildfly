package org.wildfly.test.integration.observability.opentelemetry.nocdi;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/")
public class NoCdi {

    @GET
    @Path("/json")
    @Produces({"application/json"})
    public String getJson() {
        return "{\"result\":\"Hello World\"}";
    }
}
