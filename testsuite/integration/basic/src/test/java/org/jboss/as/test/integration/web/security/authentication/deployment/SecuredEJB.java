/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.security.authentication.deployment;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@Stateless
@RolesAllowed({ "guest" })
@SecurityDomain("auth-test")
public class SecuredEJB {

    @Resource
    private SessionContext ctx;

    public String getSecurityInfo() {
        return ctx.getCallerPrincipal().toString();
    }
}
