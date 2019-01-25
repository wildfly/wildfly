package org.jboss.as.jaxrs.rsources;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("/parameterizedtype")
public class SimpleClassParameterizedTypeResource {
    @GET
    public void testQueryParam(@DefaultValue("101")
                               @QueryParam("111") List<SimpleClass> param) {
        List<SimpleClass> xparam = param;
    }
}
