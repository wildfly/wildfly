/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.weld.builtinBeans;

import jakarta.annotation.security.RunAs;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import org.jboss.ejb3.annotation.RunAsPrincipal;

@Stateless
@RunAs("Admin")
@RunAsPrincipal("non-anonymous")
public class CallerWithIdentity {

    @Inject
    BeanWithInjectedPrincipal beanA;

    @Inject
    BeanWithPrincipalFromEJBContext beanB;

    @Inject
    BeanWithSecuredPrincipal beanC;

    public String getCallerPrincipalInjected() {
        return beanA.getPrincipalName();
    }

    public String getCallerPrincipalFromEJBContext() {
        return beanB.getPrincipalName();
    }

    public String getCallerPrincipalFromEJBContextSecuredBean() {
        return beanC.getPrincipalName();
    }
}
