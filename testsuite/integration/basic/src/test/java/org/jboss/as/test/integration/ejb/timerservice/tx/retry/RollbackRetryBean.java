/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.tx.retry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Stateless
public class RollbackRetryBean {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    private static final int TIMER_CALL_WAITING_S = 10;

    private static volatile int count = 0;

    @Resource
    private TimerService timerService;

    @Resource
    private EJBContext context;

    public void createTimer() {
        timerService.createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    @Timeout
    public void timeout() {
        if(count == 0) {
            //this should be retried
            count++;
            context.setRollbackOnly();
        } else {
            count++;
            latch.countDown();
        }
    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return count > 1;
    }

}
