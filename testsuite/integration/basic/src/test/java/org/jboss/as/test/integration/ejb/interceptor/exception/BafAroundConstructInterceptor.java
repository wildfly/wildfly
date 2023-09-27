/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.exception;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@BafBinding
@Priority(1000)
@Interceptor
public class BafAroundConstructInterceptor {

    private static boolean aroundConstructCalled = false;

    @AroundConstruct
    public void intercept(InvocationContext ctx) {
        aroundConstructCalled = true;
        throw new Error("Do not suppress me.");
    }
    public static void reset() {
        aroundConstructCalled = false;
    }

    public static boolean isAroundConstructCalled() {
        return aroundConstructCalled;
    }
}
