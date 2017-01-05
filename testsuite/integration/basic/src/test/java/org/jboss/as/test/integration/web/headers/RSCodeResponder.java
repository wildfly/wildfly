/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.web.headers;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


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
        resp.getOutputStream().write((CONTENT).getBytes());
        return Response.status(Integer.parseInt(code)).build();
    }

    @GET
    @Path("server/info")
    @Produces(MediaType.TEXT_PLAIN)
    public String return204(@Context ServletContext servletContext) throws Exception{
        return servletContext.getServerInfo();
    }
}
