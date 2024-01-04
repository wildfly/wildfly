/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.exception;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@FooBinding
@Priority(1000)
@Interceptor
public class FooAroundInvokeInterceptor {

    private static boolean aroundInvokeCalled = false;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) {
        aroundInvokeCalled = true;
        throw new RuntimeException("Do not suppress me.");
    }

    public static void reset() {
        aroundInvokeCalled = false;
    }

    public static boolean isAroundInvokeCalled() {
        return aroundInvokeCalled;
    }
}
