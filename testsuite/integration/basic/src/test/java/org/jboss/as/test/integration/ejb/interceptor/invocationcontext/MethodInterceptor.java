/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.invocationcontext;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * @author OndrejChaloupka
 */
public class MethodInterceptor {
    private static final Logger log = Logger.getLogger(MethodInterceptor.class);

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        String ret = InvocationContextChecker.checkBeanInterceptorContext(ctx, "Class", "Method");
        return ret + ctx.proceed();
    }

    Object interceptTimeout(InvocationContext ctx) throws Exception {
        String ret = InvocationContextChecker.checkTimeoutInterceptorContext(ctx, "Class", "Method");
        TimeoutBean.interceptorResults += ret;
        return ctx.proceed();
    }

    @PostConstruct
    void postConstruct(InvocationContext ctx) {
        log.trace("PostConstruct on MethodInterceptor called");
        if (ctx.getMethod() != null) {
            throw new RuntimeException("InvocationContext.getMethod() on lifecycle event has to be null");
        }
    }
}
