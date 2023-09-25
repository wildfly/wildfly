/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Local({Restriction.class, FullAccess.class})
@LocalBean
public class BeanWithoutExplicitSecurityDomain implements Restriction, FullAccess {

    @Resource
    private SessionContext sessionContext;

    @Override
    @DenyAll
    public void restrictedMethod() {
        throw new RuntimeException("No one was expected to be able to call this method");
    }

    @Override
    @PermitAll
    public void doAnything() {
    }


    @RolesAllowed("Role1")
    public void allowOnlyRoleOneToAccess() {
        if (!this.sessionContext.isCallerInRole("Role1")) {
            throw new RuntimeException("Only user(s) in role1 were expected to have access to this method");
        }
    }

    @RolesAllowed("Role2")
    public void allowOnlyRoleTwoToAccess() {
        if (!this.sessionContext.isCallerInRole("Role2")) {
            throw new RuntimeException("Only user(s) in role2 were expected to have access to this method");
        }
    }
}
