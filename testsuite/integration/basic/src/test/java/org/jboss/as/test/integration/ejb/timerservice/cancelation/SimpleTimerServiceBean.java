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
package org.jboss.as.test.integration.ejb.timerservice.cancelation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SimpleTimerServiceBean {

    private static final CountDownLatch latch = new CountDownLatch(1);

    private static final int TIMER_INIT_TIME_MS = 100;
    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    // should to be greater then (timer init time + timeout time)
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
                timerExit.await();
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
