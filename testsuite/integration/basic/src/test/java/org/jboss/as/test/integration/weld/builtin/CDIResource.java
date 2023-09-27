/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.builtin;

import java.security.Principal;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("cdiInject")
@Produces({"text/plain"})
public class CDIResource {

    @Inject
    Principal principal;

    @GET
    public String getMessage() {
        return principal.getName();
    }
}
