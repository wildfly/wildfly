package org.jboss.as.test.manualmode.ejb.client.outbound.connection.security;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2017 Red Hat, Inc.
 */
@Stateless
@SecurityDomain("ejb-remote-tests")
@PermitAll
public class ServerWhoAmI implements WhoAmI {

    @Resource
    private SessionContext ctx;

    public String whoAmI() {
        return ctx.getCallerPrincipal().getName();
    }

    @RolesAllowed("admin")
    public String whoAmIRestricted() {
        return ctx.getCallerPrincipal().getName();
    }

}
