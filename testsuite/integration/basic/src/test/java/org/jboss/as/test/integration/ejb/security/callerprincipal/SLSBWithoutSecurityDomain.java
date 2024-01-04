/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * SessionBeanWithoutSecurityDomain - testing getCallerPrincipal() method
 *
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@Stateless
@Remote(ISLSBWithoutSecurityDomain.class)
public class SLSBWithoutSecurityDomain implements ISLSBWithoutSecurityDomain {
    @Resource
    private SessionContext sessContext;

    /**
     * {@inheritDoc}
     */
    public Principal getCallerPrincipal() {
        // as per the API, the getCallerPrincipal never returns null.
        // if there is no principal associated then 'anonymous' role is returned
        return this.sessContext.getCallerPrincipal();
    }

}
