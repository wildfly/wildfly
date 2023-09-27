/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.persistence;

import jakarta.annotation.Resource;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
@Singleton
public class CalendarTimerServiceBean {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_CALL_WAITING_S = 30;
    static final String MESSAGE = "Hello";

    private static volatile String message = null;

    @Resource
    private TimerService timerService;

    public void createTimer() {
        timerService.createCalendarTimer(new ScheduleExpression().second("*").minute("*").hour("*").dayOfMonth("*").year("*"), new TimerConfig(MESSAGE, true));
    }

    @Timeout
    public void timeout(Timer timer) {
        message = (String) timer.getInfo();
        latch.countDown();
    }

    public static String awaitTimerCall() {
        try {
            //on a slow machine this may take a while
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return message;
    }



}
