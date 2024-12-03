/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.cfg;

import javax.naming.NamingException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("attribute")
public class ResteasyAttributeResource {

    @Inject
    private ServletContext context;

    @Path("{param}")
    @GET
    @Produces("text/plain")
    public String getParameter(@PathParam("param") String param) throws NamingException {
        return context.getInitParameter(param);
    }
}
