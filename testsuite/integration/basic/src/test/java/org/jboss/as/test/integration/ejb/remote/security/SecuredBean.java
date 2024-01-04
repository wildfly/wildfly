/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.security;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Simple EJB to return information about the current Principal.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@Remote(SecurityInformation.class)
@SecurityDomain("other")
@PermitAll
public class SecuredBean implements SecurityInformation {

    @Resource
    private EJBContext context;

    @Override
    public String getPrincipalName() {
        return context.getCallerPrincipal().getName();
    }

}
