/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestInterceptor {
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        String message = (String) ctx.proceed();
        return TestInterceptor.class.getSimpleName() + ":" + message;
    }

}
