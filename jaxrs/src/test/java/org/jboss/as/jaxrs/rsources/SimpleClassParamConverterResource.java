package org.jboss.as.jaxrs.rsources;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/simpleclass")
public class SimpleClassParamConverterResource {
    @GET
    public void testQueryParam(@DefaultValue("101")
                               @QueryParam("111") SimpleClass param) {
        SimpleClass xparam = param;
    }
}
