/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas;

import static jakarta.ejb.TransactionAttributeType.SUPPORTS;

import java.security.Principal;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;

import org.jboss.as.test.integration.ejb.security.WhoAmI;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@LocalBean
@RolesAllowed("Role2")
@TransactionAttribute(SUPPORTS)
@SecurityDomain("ejb3-tests")
public class WhoAmIBean extends org.jboss.as.test.integration.ejb.security.base.WhoAmIBean implements WhoAmI {

    // TODO - Do I really need to override methods and do they really need to be annotated individually.

    @Override
    @RolesAllowed("Role2")
    public Principal getCallerPrincipal() {
        return super.getCallerPrincipal();
    }

    @Override
    @RolesAllowed("Role2")
    public boolean doIHaveRole(String roleName) {
        return super.doIHaveRole(roleName);
    }

    @RolesAllowed("Role1")
    public void onlyRole1() {
        throw new AssertionError("Should not come here");
    }
}
