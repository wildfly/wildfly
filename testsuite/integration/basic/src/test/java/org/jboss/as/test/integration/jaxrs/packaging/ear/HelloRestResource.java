package org.jboss.as.test.integration.jaxrs.packaging.ear;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("hellorest")
@Stateless
public class HelloRestResource {

  @GET
  @Produces({ "text/plain" })
  public String getMessage() {
    return "Hello Rest";
  }

}
