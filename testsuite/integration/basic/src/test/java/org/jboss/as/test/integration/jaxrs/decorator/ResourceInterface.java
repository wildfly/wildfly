package org.jboss.as.test.integration.jaxrs.decorator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author Stuart Douglas
 */
@Path("decorator")
@Produces({"text/plain"})
public interface ResourceInterface {

    @GET
    String getMessage();

}
