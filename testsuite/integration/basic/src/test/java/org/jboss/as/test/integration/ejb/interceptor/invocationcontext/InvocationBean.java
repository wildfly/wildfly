/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.invocationcontext;

import jakarta.ejb.Stateless;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
@Stateless
@Interceptors({ClassInterceptor.class})
public class InvocationBean {

    @Interceptors({MethodInterceptor.class})
    public String callMethod(Integer i, String str2) {
        return str2;
    }

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        String ret = InvocationContextChecker.checkBeanInterceptorContext(ctx, "Method", "Bean");
        return ret + ctx.proceed();
    }
}
