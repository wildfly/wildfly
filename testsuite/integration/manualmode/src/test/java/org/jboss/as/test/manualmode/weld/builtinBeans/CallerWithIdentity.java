/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.weld.builtinBeans;

import org.jboss.ejb3.annotation.RunAsPrincipal;

import jakarta.annotation.security.RunAs;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
@RunAs("Admin")
@RunAsPrincipal("non-anonymous")
public class CallerWithIdentity {

    @Inject
    BeanWithPrincipalFromEJBContext beanB;

    public String getCallerPrincipalFromEJBContext() {
        return beanB.getPrincipalName();
    }
}
