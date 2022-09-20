package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/primitive")
public class PrimitiveParamResource {
    @GET
    public String doGet(@QueryParam("boolean") @DefaultValue("true") boolean v) {
        return "content";
    }

    @GET
    public String doGet(@QueryParam("byte") @DefaultValue("127") byte v) {
        return "content";
    }

    @GET
    public String doGet(@QueryParam("short") @DefaultValue("32767") short v) {
        return "content";
    }

    @GET
    public String doGet(@QueryParam("int") @DefaultValue("2147483647") int v) {
        return "content";
    }

    @GET
    public String doGet(@QueryParam("long") @DefaultValue("9223372036854775807") long v) {
        return "content";
    }

    @GET
    public String doGet(@QueryParam("float") @DefaultValue("3.14159265") float v) {
        return "content";
    }

    @GET
    public String doGet(@QueryParam("double") @DefaultValue("3.14159265358979") double v) {
        return "content";
    }

    @GET
    public String doGet(@QueryParam("char") @DefaultValue("a") char v) {
         return "content";
    }
}
