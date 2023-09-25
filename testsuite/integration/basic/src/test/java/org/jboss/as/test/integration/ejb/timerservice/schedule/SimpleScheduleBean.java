/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.ScheduleExpression;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
@Stateless
public class SimpleScheduleBean {
    @Resource
    private TimerService timerService;

    private static final Logger log = Logger.getLogger(SimpleScheduleBean.class);
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile boolean timerServiceCalled = false;

    private static String timerInfo;
    private static boolean isPersistent;
    private static boolean isCalendar;
    private static String timezone;

    public String getTimerInfo() {
        return timerInfo;
    }
    public boolean isPersistent() {
        return isPersistent;
    }
    public boolean isCalendar() {
        return isCalendar;
    }
    public String getTimezone() {
        return timezone;
    }


    @Schedule(second="*", minute = "*", hour = "*", persistent = false, info = "info", timezone = "Europe/Prague")
    public void timeout(Timer timer) {
        timerInfo = (String) timer.getInfo();
        log.trace("timer info= " + timerInfo);
        isPersistent = timer.isPersistent();
        isCalendar = timer.isCalendarTimer();
        timezone = timer.getSchedule().getTimezone();
        log.trace(timer.getSchedule().getEnd());

        timerServiceCalled = true;
        latch.countDown();
    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

    @Timeout
    private void noop(Timer timer) {
    }

    /**
     * Verifies that changing timezone in a schedule expression after the timer creation
     * should not affect the previously created timer.
     */
    public void verifyTimezone() {
        final String[] zoneIds = {
                "Europe/Andorra",
                "Asia/Dubai",
                "Asia/Kabul",
                "America/Antigua",
                "America/Anguilla",
                "Africa/Johannesburg",
                "Africa/Lusaka",
                "Africa/Harare"
        };
        final ScheduleExpression exp = new ScheduleExpression().year(9999);
        final ArrayList<Timer> timers = new ArrayList<>();
        for (String z : zoneIds) {
            exp.timezone(z);
            timers.add(timerService.createCalendarTimer(exp, new TimerConfig(z, false)));
        }

        RuntimeException e = null;
        for (Timer t : timers) {
            final Serializable info = t.getInfo();
            final String timezone = t.getSchedule().getTimezone();
            if (!info.equals(timezone)) {
                e = new RuntimeException(
                    String.format("Expecting schedule expression timezone: %s, but got: %s", info, timezone));
                break;
            }
        }

        for (Timer t : timers) {
            try {
                t.cancel();
            } catch (Exception ignore) {
            }
        }
        if (e != null) {
            throw e;
        }
    }
}
