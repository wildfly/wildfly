package org.jboss.as.test.manualmode.server.nongraceful.deploymenta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("/resourcea")
public class ResourceA {
    @GET
    public String get() {
        return "Hello";
    }
}
