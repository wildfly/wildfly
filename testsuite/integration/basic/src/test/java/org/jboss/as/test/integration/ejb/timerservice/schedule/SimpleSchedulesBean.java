/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.schedule;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.Schedules;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Stateless
public class SimpleSchedulesBean {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile boolean timerServiceCalled = false;

    private static String timerInfo;
    private static boolean isPersistent;
    private static boolean isCalendar;

    @Resource
    private TimerService timerService;

    public String getTimerInfo() {
        return timerInfo;
    }
    public boolean isPersistent() {
        return isPersistent;
    }
    public boolean isCalendar() {
        return isCalendar;
    }

    @Schedules({
            @Schedule(second="0/2", minute = "*", hour = "*", info = "info"),
            @Schedule(second="1/2", minute = "*", hour = "*", info = "info"),
            @Schedule(second="1/0", info = "S0", year = "9999", persistent = false),
            @Schedule(minute = "1/0", info = "M0", year = "9999", persistent = false),
            @Schedule(hour = "0/0", info = "H0", year = "9999", persistent = false)
    })
    public void timeout(Timer timer) {
        timerInfo = (String) timer.getInfo();
        isPersistent = timer.isPersistent();
        isCalendar = timer.isCalendarTimer();

        timerServiceCalled = true;
        latch.countDown();
    }

    public Collection<Timer> getTimers() {
        return timerService.getTimers();
    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

}
