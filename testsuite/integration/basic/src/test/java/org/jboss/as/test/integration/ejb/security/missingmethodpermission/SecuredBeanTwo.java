/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.missingmethodpermission;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote (SecurityTestRemoteView.class)
public class SecuredBeanTwo implements SecurityTestRemoteView {

    @Resource
    private EJBContext ejbContext;

    @RolesAllowed("Role1")
    public String methodWithSpecificRole() {
        return ejbContext.getCallerPrincipal().getName();
    }

    public String methodWithNoRole() {
        return ejbContext.getCallerPrincipal().getName();
    }
}
