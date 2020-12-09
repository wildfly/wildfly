package org.jboss.as.test.manualmode.server.graceless.deploymenta;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/resourcea")
public class ResourceA {
    @GET
    public String get() {
        System.out.println("***** [graceless] Inside ResourceA");
        return "Hello";
    }
}