/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.persistence;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerHandle;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Singleton
public class CancelledTimerServiceBean {

    private static final CountDownLatch latch = new CountDownLatch(1);

    private static final int TIMER_CALL_WAITING_S = 30;
    private static final int TIMER_CALL_QUICK_WAITING_S = 2;
    private static volatile boolean timerServiceCalled = false;

    @Resource
    private TimerService timerService;

    public TimerHandle createTimer() {
        ScheduleExpression expression = new ScheduleExpression();
        expression.second("*");
        expression.minute("*");
        expression.hour("*");
        expression.dayOfMonth("*");
        expression.year("*");
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(new String("info"));
        return timerService.createCalendarTimer(expression, timerConfig).getHandle();
    }

    @Timeout
    public void timeout() {
        timerServiceCalled = true;
        latch.countDown();
    }

    public static boolean awaitTimerCall() {
        try {
            //on a slow machine this may take a while
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

    public static boolean quickAwaitTimerCall() {
        try {
            latch.await(TIMER_CALL_QUICK_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

}
