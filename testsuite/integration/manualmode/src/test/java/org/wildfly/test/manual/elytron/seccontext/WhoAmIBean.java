/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import java.security.Principal;
import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * Stateless implementation of the {@link WhoAmI}.
 * @author Josef Cacek
 */
@Stateless
@RolesAllowed({ "whoami", "admin", "no-server2-identity", "authz" })
@DeclareRoles({ "entry", "whoami", "servlet", "admin", "no-server2-identity", "authz" })
public class WhoAmIBean implements WhoAmI {

    @Resource
    private SessionContext context;

    @Override
    public Principal getCallerPrincipal() {
        return context.getCallerPrincipal();
    }

    @Override
    public String throwIllegalStateException() {
        throw new IllegalStateException("Expected IllegalStateException from WhoAmIBean.");
    }

    @Override
    public String throwServer2Exception() {
        throw new Server2Exception("Expected Server2Exception from WhoAmIBean.");
    }

}
