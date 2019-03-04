package org.jboss.as.test.integration.jaxrs.packaging.ear;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("hellorest")
@Stateless
public class HelloRestResource {

  @GET
  @Produces({ "text/plain" })
  public String getMessage() {
    return "Hello Rest";
  }

}