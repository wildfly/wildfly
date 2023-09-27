/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.as.ejb3.context.SessionContextImpl;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.authz.Roles;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless
@RunAs("nobody")
@SecurityDomain("ejb3-tests")
public class UncheckedStatelessBean {
    private static final Logger log = Logger.getLogger(UncheckedStatelessBean.class);

    @Resource
    private SessionContext ctx;

    @PermitAll
    public Set<Principal> unchecked() {
        org.wildfly.security.auth.server.SecurityDomain securityDomain = org.wildfly.security.auth.server.SecurityDomain.getCurrent();
        if (securityDomain != null) {
            final Roles roles = ((SessionContextImpl) ctx).getComponent().getIncomingRunAsIdentity().getRoles("ejb");
            final HashSet<Principal> rolesSet = new HashSet<>();
            if (roles != null) {
                roles.forEach(role -> rolesSet.add(new NamePrincipal(role.toString())));
            }
            return rolesSet;
        } else {
            throw new IllegalStateException("Legacy security has been removed");
        }
    }
}
