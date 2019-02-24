package org.jboss.as.jaxrs.rsources;

import javax.ws.rs.GET;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("fromString")
public class SimpleFromStringResource {
    @GET
    public String get(@DefaultValue("defaulFromString")
                      @QueryParam("newString") SimpleFromStringProvider p) {
        return "done";
    }
}
