/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.propagation;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;
/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@Stateless
@SecurityDomain("BusinessDomain")
public class WhoAmIBeanRemote implements WhoAmIRemote {

    @Resource
    private SessionContext context;

    @PermitAll
    public String getCallerPrincipal() {
        return context.getCallerPrincipal().getName();
    }

    @PermitAll
    public boolean isCallerInRole(String roleName) {
        return context.isCallerInRole(roleName);
    }
    // rls debug
    @PermitAll
    public String getSessionContext() {
        return ""+ context;
    }
}
