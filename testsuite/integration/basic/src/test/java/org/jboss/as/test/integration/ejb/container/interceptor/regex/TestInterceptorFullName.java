/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor.regex;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class TestInterceptorFullName {
    public static boolean invoked = false;

    @AroundInvoke
    public Object invoke(final InvocationContext invocationContext) throws Exception {
        invoked = true;
        return invocationContext.proceed();
    }
}
