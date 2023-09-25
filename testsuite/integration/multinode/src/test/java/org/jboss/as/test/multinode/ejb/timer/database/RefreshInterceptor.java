/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.ejb.timer.database;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor to enable programmatic timer refresh across multiple nodes.
 */
@Interceptor
public class RefreshInterceptor {
    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        context.getContextData().put("wildfly.ejb.timer.refresh.enabled", Boolean.TRUE);
        return context.proceed();
    }
}
