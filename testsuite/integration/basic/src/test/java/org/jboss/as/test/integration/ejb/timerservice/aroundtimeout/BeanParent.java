/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.aroundtimeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * @author Stuart Douglas
 */
public class BeanParent {
    private static final CountDownLatch latch = new CountDownLatch(1);

    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    // has to be greater than timeout time
    private static final int TIMER_CALL_WAITING_MS = 30000;
    private static volatile boolean timerServiceCalled = false;

    @Resource
    private TimerService timerService;

    public void createTimer() {
        timerService.createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    @AroundTimeout
    public Object aroundTimeoutParent(final InvocationContext context) throws Exception {
        InterceptorOrder.intercept(BeanParent.class);
        return  context.proceed();
    }

    @Timeout
    @Interceptors(MethodInterceptorChild.class)
    private void timeout(Timer timer) {
        timerServiceCalled = true;
        latch.countDown();
    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }
}
