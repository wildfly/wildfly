/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.managedbean;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * @author John Bailey
 */
@Interceptors(InterceptorFromParent.class)
public class BeanParent {
    @AroundInvoke
    public Object interceptParent(InvocationContext context) throws Exception {
        if (!context.getMethod().getName().equals("echo")) {
            return context.proceed();
        }
        return "#BeanParent#" + context.proceed();
    }
}
