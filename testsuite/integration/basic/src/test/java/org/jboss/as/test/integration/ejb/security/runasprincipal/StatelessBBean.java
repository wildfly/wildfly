/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Local;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@Local(WhoAmI.class)
@RolesAllowed("Admin")
@SecurityDomain("other")
public class StatelessBBean implements WhoAmI {
    @Resource
    private SessionContext ctx;

    public String getCallerPrincipal() {
        return ctx.getCallerPrincipal().getName();
    }
}
