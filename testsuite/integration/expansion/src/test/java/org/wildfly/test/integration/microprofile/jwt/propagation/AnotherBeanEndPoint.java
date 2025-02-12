/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.propagation;

import java.security.Principal;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * A simple Jakarta RESTful Web Services endpoint deployed as a Jakarta Enterprise Beans bean.
 *
 * @author <a href="fjuma@redhat.com">Farah Juma</a>
 */
@Path("/AnotherSample")
@Stateless
public class AnotherBeanEndPoint {

    @Inject
    JsonWebToken jwt;

    @EJB
    AnotherTargetBean anotherTargetBean;

    @GET()
    @Path("/subscription")
    public String helloRolesAllowed(@Context SecurityContext ctx) {
        Principal caller = ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        boolean hasJWT = jwt.getClaimNames() != null;
        String helloReply = String.format("hello + %s, hasJWT: %s, targetCallerPrincipal: %s, targetIsCallerAdmin: %b", name, hasJWT, anotherTargetBean.getCallerPrincipal(), anotherTargetBean.isCallerInRole("Admin"));

        return helloReply;
    }

}
