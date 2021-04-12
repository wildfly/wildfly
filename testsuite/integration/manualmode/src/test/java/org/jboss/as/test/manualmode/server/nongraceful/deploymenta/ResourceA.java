package org.jboss.as.test.manualmode.server.nongraceful.deploymenta;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/resourcea")
public class ResourceA {
    @GET
    public String get() {
        return "Hello";
    }
}
