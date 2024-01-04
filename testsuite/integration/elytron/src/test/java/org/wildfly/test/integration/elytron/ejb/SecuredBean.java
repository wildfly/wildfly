/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Simple Jakarta Enterprise Beans to return information about the current Principal.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@Remote(SecurityInformation.class)
@SecurityDomain("elytron-tests")
@RolesAllowed("Users")
public class SecuredBean implements SecurityInformation {

    @Resource
    private EJBContext context;

    @Override
    public String getPrincipalName() {
        return context.getCallerPrincipal().getName();
    }

}
