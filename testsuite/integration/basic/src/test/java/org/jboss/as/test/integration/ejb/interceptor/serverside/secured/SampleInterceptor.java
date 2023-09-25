/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.secured;

import java.util.concurrent.CountDownLatch;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class SampleInterceptor {
    public static final CountDownLatch latch = new CountDownLatch(SecuredBeanTestCase.EJB_INVOKED_METHODS_COUNT);

    public SampleInterceptor() {
    }

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        try {
            return invocationContext.proceed();
        } finally {
            latch.countDown();
        }
    }
}
