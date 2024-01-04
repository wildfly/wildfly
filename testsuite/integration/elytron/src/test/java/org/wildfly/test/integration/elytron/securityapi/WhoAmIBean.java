/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.securityapi;

import java.security.Principal;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@SecurityDomain("other")
public class WhoAmIBean implements WhoAmI {

    @Resource
    private SessionContext sessionContext;

    @Inject
    private SecurityContext securityContext;

    @Override
    public Principal getCallerPrincipalSessionContext() {
        return sessionContext.getCallerPrincipal();
    }

    @Override
    public Principal getCallerPrincipalSecurityDomain() {
        return org.wildfly.security.auth.server.SecurityDomain.getCurrent().getCurrentSecurityIdentity().getPrincipal();
    }

    @Override
    public Principal getCallerPrincipalSecurityContext() {
        return securityContext.getCallerPrincipal();
    }
}
