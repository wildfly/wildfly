/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.exceptions;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@ExceptionBinding
public class ExceptionInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }

}
