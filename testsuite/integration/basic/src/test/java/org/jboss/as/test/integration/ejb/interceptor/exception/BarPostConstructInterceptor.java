/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.exception;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@BarBinding
@Priority(1000)
@Interceptor
public class BarPostConstructInterceptor {

    private static boolean postConstructCalled = false;

    @PostConstruct
    public void intercept(InvocationContext ctx) {
        postConstructCalled = true;
        throw new IllegalStateException("Do not suppress me.");
    }
    public static void reset() {
        postConstructCalled = false;
    }

    public static boolean isPostConstructCalled() {
        return postConstructCalled;
    }
}
