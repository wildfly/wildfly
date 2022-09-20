package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/simpleclass")
public class SimpleClassParamConverterResource {
    @GET
    public void testQueryParam(@DefaultValue("101")
                               @QueryParam("111") SimpleClass param) {
        SimpleClass xparam = param;
    }
}
