/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.authorization;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 */
@Stateless
@Remote(SaslLegacyMechanismBeanRemote.class)
@PermitAll
public class SaslLegacyMechanismBean implements SaslLegacyMechanismBeanRemote {

    @Resource
    private EJBContext context;

    @Override
    public String getPrincipal() {
        return context.getCallerPrincipal().getName();
    }
}
