/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/secure")
public class SecureResource {

    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String admin() {
        return "admin";
    }

    @GET
    @Path("/manager")
    @RolesAllowed({"admin", "manager"})
    public String manager() {
        return "manager";
    }

    @GET
    @Path("/user")
    @RolesAllowed({"admin", "manager", "user"})
    public String user() {
        return "user";
    }
}
