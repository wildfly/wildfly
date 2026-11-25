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
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerHandle;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CalendarTimerServiceBean {

    // should be greater than one second (1000ms)
    private static final int TIMER_CALL_QUICK_WAITING_MS = 1200;

    private static final CountDownLatch latch = new CountDownLatch(1);

    private static volatile boolean timerServiceCalled = false;
    boolean first = true;

    private final CountDownLatch timerEntry = new CountDownLatch(1);
    private final CountDownLatch timerExit = new CountDownLatch(1);


    public TimerHandle createTimer() {
        ScheduleExpression expression = new ScheduleExpression();
        expression.second("*");
        expression.minute("*");
        expression.hour("*");
        expression.dayOfMonth("*");
        expression.year("*");
        return timerService.createCalendarTimer(expression).getHandle();
    }
    @Resource
    private TimerService timerService;

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
