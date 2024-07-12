/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.propagation.sameVirtualDomain;

import java.security.Principal;

import javax.naming.InitialContext;
import javax.naming.NamingException;

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
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@Path("/Sample")
@Stateless
public class RemoteBeanEndPoint {

    @Inject
    JsonWebToken jwt;

    @GET()
    @Path("/subscription")
    public String helloRolesAllowed(@Context SecurityContext ctx) {
        WhoAmIRemote whoAmIRemote = lookup(WhoAmIRemote.class, "java:global/ear-deployment-ejb-same-domain/ear-deployment-ejb-same-domain-ejb/WhoAmIBeanRemote!org.wildfly.test.integration.microprofile.jwt.propagation.sameVirtualDomain.WhoAmIRemote");
        Principal caller = ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        boolean hasJWT = jwt.getClaimNames() != null;
        String helloReply = String.format("hello + %s, hasJWT: %s, targetCallerPrincipal: %s, hasAdminRole: %b, hasSubscriberRole: %b", name, hasJWT, whoAmIRemote.getCallerPrincipal(), whoAmIRemote.isCallerInRole("Admin"), whoAmIRemote.isCallerInRole("Subscriber"));

        return helloReply;
    }

    public static <T> T lookup(Class<T> clazz, String jndiName) {
        Object bean = lookup(jndiName);
        return clazz.cast(bean);
    }

    private static Object lookup(String jndiName) {
        javax.naming.Context context = null;
        try {
            context = new InitialContext();
            return context.lookup(jndiName);
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        } finally {
            try {
                context.close();
            } catch (NamingException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

}
