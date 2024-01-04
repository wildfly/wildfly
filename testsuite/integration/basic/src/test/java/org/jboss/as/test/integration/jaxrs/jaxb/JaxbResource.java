/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jaxb;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("jaxb")
@Produces({"application/xml"})
public class JaxbResource {
    @GET
    public JaxbModel get() {
        return new JaxbModel("John","Citizen");
    }
}
