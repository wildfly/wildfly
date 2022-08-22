package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("fromValue")
public class SimpleFromValueResource {
    @GET
    public String get(@DefaultValue("defaulFromValue")
                      @QueryParam("newValue") SimpleFromValueProvider p) {
        return "done";
    }
}
