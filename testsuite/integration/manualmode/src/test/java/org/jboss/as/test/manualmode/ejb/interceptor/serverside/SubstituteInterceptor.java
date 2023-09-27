/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ejb.interceptor.serverside;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * A simple interceptor that adds string prefix to the intercepted method return value.
 */
public class SubstituteInterceptor {

    static final String PREFIX = "Intercepted:";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        return PREFIX + invocationContext.proceed();
    }
}
