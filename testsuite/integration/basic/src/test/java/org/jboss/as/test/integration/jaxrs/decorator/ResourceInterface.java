package org.jboss.as.test.integration.jaxrs.decorator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author Stuart Douglas
 */
@Path("decorator")
@Produces({"text/plain"})
public interface ResourceInterface {

    @GET
    String getMessage();

}
