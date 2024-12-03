/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * The interceptor responsible for managing the current {@link org.jboss.as.ee.concurrent.ConcurrentContext}.
 *
 * @author Eduardo Martins
 */
public class ConcurrentContextInterceptor implements Interceptor {

    private final ConcurrentContext concurrentContext;

    public ConcurrentContextInterceptor(ConcurrentContext concurrentContext) {
        this.concurrentContext = concurrentContext;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        ConcurrentContext.pushCurrent(concurrentContext);
        try {
            return context.proceed();
        } finally {
            ConcurrentContext.popCurrent();
        }
    }

}