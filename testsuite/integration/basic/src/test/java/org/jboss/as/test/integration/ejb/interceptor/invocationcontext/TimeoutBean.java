/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.invocationcontext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.ExcludeDefaultInterceptors;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * @author Ondrej Chaloupka
 */
@Stateless
@Interceptors({ClassInterceptor.class})
public class TimeoutBean {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static boolean timerServiceCalled = false;
    public static String interceptorResults = "";

    @Resource
    private TimerService timerService;

    @ExcludeDefaultInterceptors
    @ExcludeClassInterceptors
    public void createTimer() {
        timerService.createTimer(100, null);
    }

    @AroundTimeout
    public Object aroundTimeoutParent(final InvocationContext ctx) throws Exception {
        String ret = InvocationContextChecker.checkTimeoutInterceptorContext(ctx, "Method", "Bean");
        TimeoutBean.interceptorResults += ret;
        return ctx.proceed();
    }

    @Timeout
    @Interceptors(MethodInterceptor.class)
    private void timeout(Timer timer) {
        timerServiceCalled = true;
        interceptorResults += "@Timeout";
        latch.countDown();
    }

    @ExcludeDefaultInterceptors
    @ExcludeClassInterceptors
    public static boolean awaitTimerCall() {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }
}
