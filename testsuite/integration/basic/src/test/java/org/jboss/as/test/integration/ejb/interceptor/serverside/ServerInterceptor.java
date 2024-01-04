/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside;

import java.util.concurrent.CountDownLatch;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

/**
 * Server side sample interceptor
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
public class ServerInterceptor {

    public static final CountDownLatch latch = new CountDownLatch(1);
    public static final CountDownLatch timeoutLatch = new CountDownLatch(1);

    public ServerInterceptor() {
    }

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        try {
            return invocationContext.proceed();
        } finally {
            latch.countDown();
        }
    }

    @AroundTimeout
    public Object aroundTimeout(final InvocationContext invocationContext) throws Exception {
        try {
            return invocationContext.proceed();
        } finally {
            timeoutLatch.countDown();
        }
    }
}
