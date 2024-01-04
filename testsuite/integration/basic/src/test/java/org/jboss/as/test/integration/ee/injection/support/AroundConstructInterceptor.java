/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@AroundConstructBinding
@Priority(Interceptor.Priority.APPLICATION + 5)
public class AroundConstructInterceptor {

    public static boolean aroundConstructCalled = false;

    private static String prefix = "AroundConstructInterceptor#";

    @AroundConstruct
    public Object intercept(InvocationContext ctx) throws Exception {
        aroundConstructCalled = true;
        Object[] params = ctx.getParameters();
        if (params.length > 0) {
            params[0] = prefix + params[0];
        }
        return ctx.proceed();
    }

    public static void reset() {
        aroundConstructCalled = false;
    }
}
