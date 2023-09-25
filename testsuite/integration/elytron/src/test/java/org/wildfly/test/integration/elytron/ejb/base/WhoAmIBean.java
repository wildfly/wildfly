/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb.base;

import java.security.Principal;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */

public abstract class WhoAmIBean {

    @Resource
    private SessionContext context;

    public Principal getCallerPrincipal() {
        return context.getCallerPrincipal();
    }

    public boolean doIHaveRole(String roleName) {
        return context.isCallerInRole(roleName);
    }
}
