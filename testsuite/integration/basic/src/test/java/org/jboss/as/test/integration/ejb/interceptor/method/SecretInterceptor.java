/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.method;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class SecretInterceptor {

    public static boolean called = false;
    public static boolean postConstructCalled = false;

    @PostConstruct
    public void postConstruct(final InvocationContext invocationContext) throws Exception {
        postConstructCalled = true;
        invocationContext.proceed();
    }

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        called = true;
        return invocationContext.proceed();
    }
}
