/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runasprincipal;

import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@Remote(WhoAmI.class)
@RunAs("Admin")
@RunAsPrincipal("jackinabox")
@SecurityDomain("other")
public class CallerWithIdentity implements WhoAmI {
    @EJB(beanName = "StatelessABean")
    private WhoAmI beanA;

    public String getCallerPrincipal() {
        return beanA.getCallerPrincipal();
    }
}
