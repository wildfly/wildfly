/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.invocationcontext;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * @author OndrejChaloupka
 */
public class ClassInterceptor {
    private static final Logger log = Logger.getLogger(ClassInterceptor.class);

    @AroundInvoke
    Object intercept(InvocationContext ctx) throws Exception {
        String ret = InvocationContextChecker.checkBeanInterceptorContext(ctx, "Default", "Class");
        return ret + ctx.proceed();
    }

    @AroundTimeout
    Object interceptTimeout(InvocationContext ctx) throws Exception {
        String ret = InvocationContextChecker.checkTimeoutInterceptorContext(ctx, "Default", "Class");
        TimeoutBean.interceptorResults += ret;
        return ctx.proceed();
    }

    @PostConstruct
    void postConstruct(InvocationContext ctx) {
        log.trace("PostConstruct on ClassInterceptor called");
        if (ctx.getMethod() != null) {
            throw new RuntimeException("InvocationContext.getMethod() on lifecycle event has to be null");
        }
    }
}
