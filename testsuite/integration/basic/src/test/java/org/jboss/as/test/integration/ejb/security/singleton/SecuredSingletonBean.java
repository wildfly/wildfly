/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.singleton;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * User: jpai
 */
@Singleton
@SecurityDomain("other")
@Remote(SingletonSecurity.class)
public class SecuredSingletonBean implements SingletonSecurity {

    @Resource
    private EJBContext ejbContext;

    @Override
    @RolesAllowed("Role1")
    public void allowedForRole1() {
        if (!this.ejbContext.isCallerInRole("Role1")) {
            throw new RuntimeException("Only Role1 was expected to be allowed to invoke this method");
        }
    }
}
