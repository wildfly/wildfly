/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.gzip;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.annotations.GZIP;

@Path("helloworld")
public class GZIPResource {
    @GET
    @GZIP
    public String getMessage() {
        return "Hello World!";
    }

    @GET
    @GZIP
    @Path("xml")
    @Produces({"application/xml"})
    public JaxbModel getXml() {
        return new JaxbModel("John","Citizen");
    }
}
