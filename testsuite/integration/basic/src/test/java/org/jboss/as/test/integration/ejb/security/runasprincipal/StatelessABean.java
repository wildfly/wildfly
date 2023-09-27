/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@Local(WhoAmI.class)
@RolesAllowed("Admin")
@SecurityDomain("other")
public class StatelessABean implements WhoAmI {
    @EJB(beanName = "StatelessBBean")
    private WhoAmI beanB;

    public String getCallerPrincipal() {
        return beanB.getCallerPrincipal();
    }
}
