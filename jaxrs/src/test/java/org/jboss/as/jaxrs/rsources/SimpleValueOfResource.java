package org.jboss.as.jaxrs.rsources;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("valueof")
public class SimpleValueOfResource {
    @GET
    public String get(@DefaultValue("defaulValueOf")
                      @QueryParam("newValueOf") SimpleValueOfProvider p) {
        return "done";
    }
}
