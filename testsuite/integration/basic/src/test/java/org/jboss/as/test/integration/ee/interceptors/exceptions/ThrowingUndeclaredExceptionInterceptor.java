/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.interceptors.exceptions;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ThrowingUndeclaredExceptionInterceptor {
    public static class SurpriseException extends Exception {
        public SurpriseException(String message) {
            super(message);
        }
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ctx) throws Exception {
        throw new SurpriseException("didn't expect this");
    }
}
