/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.multiple;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Simple interceptor imitating forbidden EJB access
 */
public class ExceptionThrowingInterceptor {

    @AroundInvoke
    public Object throwException(InvocationContext invocationContext) throws Exception {
        throw new IllegalArgumentException("Intercepted: throwing an exception.");
    }
}
