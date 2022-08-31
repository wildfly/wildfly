package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("valueof")
public class SimpleValueOfResource {
    @GET
    public String get(@DefaultValue("defaulValueOf")
                      @QueryParam("newValueOf") SimpleValueOfProvider p) {
        return "done";
    }
}
