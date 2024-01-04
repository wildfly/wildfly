/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.headers;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


/**
 * @author baranowb
 */
@Path("/test")
public class RSCodeResponder {
    public static final String CONTENT = "{\"employees\":["+
            "{\"firstName\":\"John\", \"lastName\":\"Doe\"},"+
            "{\"firstName\":\"Anna\", \"lastName\":\"Smith\"},"+
            "{\"firstName\":\"Peter\", \"lastName\":\"Jones\"}"+
         "]}";
    @GET
    @Path("returnCode/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response return204(@PathParam("code") final String code,@Context HttpServletResponse resp) throws Exception{
        resp.setContentType("application/json");
        resp.getOutputStream().write((CONTENT).getBytes(StandardCharsets.UTF_8));
        return Response.status(Integer.parseInt(code)).build();
    }

    @GET
    @Path("server/info")
    @Produces(MediaType.TEXT_PLAIN)
    public String return204(@Context ServletContext servletContext) throws Exception{
        return servletContext.getServerInfo();
    }
}
