/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.propagation.local;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@Stateless
@SecurityDomain("another-ejb-domain")
public class AnotherEntryBeanLocal implements AnotherEntryLocal {

    @Resource
    private SessionContext context;

    public String whoAmI() {
        return context.getCallerPrincipal().getName();
    }

    public boolean doIHaveRole(String roleName) {
        return context.isCallerInRole(roleName);
    }

}

