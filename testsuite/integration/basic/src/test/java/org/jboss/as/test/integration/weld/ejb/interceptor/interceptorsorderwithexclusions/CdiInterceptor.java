/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.interceptor.interceptorsorderwithexclusions;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Marius Bogoevici
 */
@Interceptor
@Secured
public class CdiInterceptor {
    static int count;

    @AroundInvoke
    public Object doSecured(InvocationContext invocationContext) throws Exception {
        count = Counter.next();
        return invocationContext.proceed();

    }
}
