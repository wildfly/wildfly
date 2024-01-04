/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.regex;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
@Interceptor
public class RegexInterceptor {
    public static final String MESSAGE = "-regex";

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        return invocationContext.proceed() + MESSAGE;
    }
}
