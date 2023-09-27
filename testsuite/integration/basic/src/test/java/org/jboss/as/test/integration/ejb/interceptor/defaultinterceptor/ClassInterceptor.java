/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class ClassInterceptor {
    public static final String MESSAGE = "ClassInterceptor ";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        if (context.getMethod().getReturnType().equals(String.class)) {
            return MESSAGE + context.proceed().toString();
        }
        return context.proceed();
    }
}
