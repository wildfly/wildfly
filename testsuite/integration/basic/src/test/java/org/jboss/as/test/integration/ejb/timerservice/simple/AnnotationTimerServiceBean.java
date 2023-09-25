/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.simple;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
@Stateless
public class AnnotationTimerServiceBean {
    private static final Logger log = Logger.getLogger(AnnotationTimerServiceBean.class);
    private static volatile CountDownLatch latch = new CountDownLatch(1);

    private static volatile boolean timerServiceCalled = false;
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile String timerInfo;
    private static volatile boolean isPersistent;
    private static volatile boolean isCalendar;

    @Resource
    private SessionContext sessionContext;

    private TimerService timerService;

    public synchronized TimerService getTimerService() {
        if(timerService == null) {
            timerService = (TimerService) sessionContext.lookup("java:comp/TimerService");
        }
        return timerService;
    }

    public void resetTimerServiceCalled() {
        timerServiceCalled = false;
        latch = new CountDownLatch(1);
    }

    public String getTimerInfo() {
        return timerInfo;
    }
    public boolean isPersistent() {
        return isPersistent;
    }
    public boolean isCalendar() {
        return isCalendar;
    }

    @Timeout
    public void timeout(Timer timer) {
        log.trace("Timer is: " + timer + ", timer info is: " + timer.getInfo());
        timerInfo = new String((String) timer.getInfo());
        isPersistent = timer.isPersistent();
        isCalendar = timer.isCalendarTimer();

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

}
