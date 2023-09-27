/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal.customdomain;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Entry WhoAmI implementation, uses default security domain and calls method on TargetBean which uses another domain.
 *
 * @author Josef Cacek
 * @see TargetBean
 */
@Stateless
@Remote(WhoAmI.class)
@DeclareRoles({ "guest", "Target" })
@RunAs("Target")
@RunAsPrincipal("principalFromEntryBean")
@SecurityDomain("other")
public class EntryBean implements WhoAmI {

    @EJB(beanName = "TargetBean")
    private WhoAmI target;

    @Resource
    private SessionContext ctx;

    @RolesAllowed("guest")
    public String getCallerPrincipal() {
        return target.getCallerPrincipal();
    }
}
