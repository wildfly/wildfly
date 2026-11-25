/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.ejb;

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
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Path("/Sample")
@Stateless
public class BeanEndPoint {

    @Inject
    JsonWebToken jwt;

    @EJB
    TargetBean targetBean;

    @GET()
    @Path("/subscription")
    public String helloRolesAllowed(@Context SecurityContext ctx) {
        Principal caller = ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        boolean hasJWT = jwt.getClaimNames() != null;
        String helloReply = String.format("hello + %s, hasJWT: %s, canCallTarget: %b", name, hasJWT, targetBean.successfulCall());

        return helloReply;
    }

}
