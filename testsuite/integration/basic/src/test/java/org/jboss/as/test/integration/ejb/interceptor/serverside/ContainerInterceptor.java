/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside;

import org.jboss.logging.Logger;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class ContainerInterceptor {

    private static final Logger logger = Logger.getLogger(ContainerInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
