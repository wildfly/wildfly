/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.managedbean;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Jozef Hartinger
 */
@Interceptor
@CDIBinding
@Priority(Interceptor.Priority.APPLICATION + 50)
public class CDIInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        if (!ctx.getMethod().getName().equals("echo")) {
            return ctx.proceed();
        }
        return "#CDIInterceptor#" + ctx.proceed();
    }
}
