/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ejb.interceptor.serverside;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class UpdatedInterceptor {
    static final String PREFIX = "UpdatedInterceptor:";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        return PREFIX + invocationContext.proceed();
    }
}

