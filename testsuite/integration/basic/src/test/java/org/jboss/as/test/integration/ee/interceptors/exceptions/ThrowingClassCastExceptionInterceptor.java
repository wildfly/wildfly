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
public class ThrowingClassCastExceptionInterceptor {
    @AroundInvoke
    public Object aroundInvoke(final InvocationContext ctx) throws Exception {
        throw new ClassCastException("test");
    }
}
