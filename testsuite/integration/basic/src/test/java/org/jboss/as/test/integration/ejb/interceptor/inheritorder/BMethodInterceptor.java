/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inheritorder;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
public class BMethodInterceptor {
    @AroundInvoke
    public Object intercept(final InvocationContext context) throws Exception {
        return "Bmethod " + context.proceed().toString();
    }
}
