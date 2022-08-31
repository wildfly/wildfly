package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;

@Path("/parameterizedtype")
public class SimpleClassParameterizedTypeResource {
    @GET
    public void testQueryParam(@DefaultValue("101")
                               @QueryParam("111") List<SimpleClass> param) {
        List<SimpleClass> xparam = param;
    }
}
