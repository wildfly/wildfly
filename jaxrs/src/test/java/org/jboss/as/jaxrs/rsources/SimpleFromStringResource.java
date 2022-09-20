package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("fromString")
public class SimpleFromStringResource {
    @GET
    public String get(@DefaultValue("defaulFromString")
                      @QueryParam("newString") SimpleFromStringProvider p) {
        return "done";
    }
}
