/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.security.runas;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.as.ejb3.context.SessionContextImpl;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;
import org.jboss.security.RunAsIdentity;
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
            // elytron profile is enabled
            final Roles roles = ((SessionContextImpl) ctx).getComponent().getIncomingRunAsIdentity().getRoles("ejb");
            final HashSet<Principal> rolesSet = new HashSet<>();
            if (roles != null) {
                roles.forEach(role -> rolesSet.add(new NamePrincipal(role.toString())));
            }
            return rolesSet;
        } else {
            // use legacy approach
            RunAsIdentity rs = (RunAsIdentity) ctx.getCallerPrincipal();
            return rs.getRunAsRoles();
        }
    }
}
