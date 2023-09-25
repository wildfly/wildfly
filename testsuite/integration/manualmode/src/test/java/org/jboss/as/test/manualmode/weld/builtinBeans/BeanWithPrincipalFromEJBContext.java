/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.weld.builtinBeans;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;

@Stateless
public class BeanWithPrincipalFromEJBContext {

    @Resource
    private EJBContext ctx;

    public String getPrincipalName() {
        return ctx.getCallerPrincipal().getName();
    }
}
