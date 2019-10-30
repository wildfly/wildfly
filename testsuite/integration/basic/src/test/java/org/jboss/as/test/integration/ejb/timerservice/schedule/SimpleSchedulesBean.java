/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.timerservice.schedule;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;

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
            @Schedule(second="1/2", minute = "*", hour = "*", info = "info")
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
