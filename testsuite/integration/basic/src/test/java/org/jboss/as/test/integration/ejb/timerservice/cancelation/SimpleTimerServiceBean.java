/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.cancelation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerHandle;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SimpleTimerServiceBean {

    private static final CountDownLatch latch = new CountDownLatch(1);

    private static final int TIMER_INIT_TIME_MS = 100;
    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    // should be greater than (timer init time + timeout time)
    private static final int TIMER_CALL_QUICK_WAITING_MS = 1000;
    private static volatile boolean timerServiceCalled = false;
    boolean first = true;

    private final CountDownLatch timerEntry = new CountDownLatch(1);
    private final CountDownLatch timerExit = new CountDownLatch(1);


    @Resource
    private TimerService timerService;

    public TimerHandle createTimer() {
        return timerService.createTimer(TIMER_INIT_TIME_MS, TIMER_TIMEOUT_TIME_MS, null).getHandle();
    }

    @Timeout
    public void timeout() {
        if (first) {
            timerEntry.countDown();
            try {
                timerExit.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            first = false;
        } else {
            timerServiceCalled = true;
            latch.countDown();
        }
    }

    public int getTimerCount() {
        return timerService.getTimers().size();
    }

    public static boolean quickAwaitTimerCall() {
        try {
            latch.await(TIMER_CALL_QUICK_WAITING_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

    public CountDownLatch getTimerEntry() {
        return timerEntry;
    }

    public CountDownLatch getTimerExit() {
        return timerExit;
    }
}
