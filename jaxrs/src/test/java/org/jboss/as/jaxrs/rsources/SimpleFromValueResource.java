package org.jboss.as.jaxrs.rsources;

import javax.ws.rs.GET;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("fromValue")
public class SimpleFromValueResource {
    @GET
    public String get(@DefaultValue("defaulFromValue")
                      @QueryParam("newValue") SimpleFromValueProvider p) {
        return "done";
    }
}
