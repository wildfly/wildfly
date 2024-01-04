/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.jaxrs;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */
@Produces({"text/plain"})
public class SubHelloResource {

    @GET
    public String hi() {
        return "Hi";
    }

    @GET
    @Path("ping/{name}")
    public String ping(@PathParam("name") @DefaultValue("JBoss") String name) {
        return "ping " + name;
    }
}
