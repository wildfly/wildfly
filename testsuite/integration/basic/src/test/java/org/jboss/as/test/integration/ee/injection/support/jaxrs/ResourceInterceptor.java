/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.jaxrs;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;

@Interceptor
@Priority(900)
@ComponentInterceptorBinding
public class ResourceInterceptor {

    @AroundInvoke
    public Object intercept(final InvocationContext invocationContext) throws Exception {
        if (invocationContext.getMethod().getName().equals("getMessage")) {
            return invocationContext.proceed() + " World";
        }
        return invocationContext.proceed();
    }

}
