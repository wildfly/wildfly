/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal.customdomain;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Local;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Target {@link WhoAmI} interface implementation, which uses a custom security domain.
 *
 * @author Josef Cacek
 * @see EntryBean
 */
@Stateless
@Local(WhoAmI.class)
@SecurityDomain("runasprincipal-test")
public class TargetBean implements WhoAmI {
    @Resource
    private SessionContext ctx;

    @RolesAllowed("Target")
    public String getCallerPrincipal() {
        return ctx.getCallerPrincipal().getName();
    }
}
