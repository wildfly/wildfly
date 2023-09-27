/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.weld.builtinBeans;

import jakarta.ejb.Stateless;
import jakarta.ejb.EJBContext;
import jakarta.annotation.Resource;

@Stateless
public class BeanWithPrincipalFromEJBContext {

    @Resource
    private EJBContext ctx;

    public String getPrincipalName() {
        return ctx.getCallerPrincipal().getName();
    }
}
