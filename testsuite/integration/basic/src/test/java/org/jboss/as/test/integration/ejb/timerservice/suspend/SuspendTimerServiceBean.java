/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.suspend;

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
public class SuspendTimerServiceBean {
    private static final Logger log = Logger.getLogger(SuspendTimerServiceBean.class);
    private static volatile CountDownLatch latch = new CountDownLatch(1);

    private static volatile int timerServiceCount = 0;
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile String timerInfo;

    @Resource
    private SessionContext sessionContext;

    private TimerService timerService;

    public synchronized TimerService getTimerService() {
        if(timerService == null) {
            timerService = (TimerService) sessionContext.lookup("java:comp/TimerService");
        }
        return timerService;
    }

    public static void resetTimerServiceCalled() {
        timerServiceCount = 0;
        latch = new CountDownLatch(1);
    }

    public String getTimerInfo() {
        return timerInfo;
    }

    @Timeout
    public synchronized void timeout(Timer timer) {
        log.trace("Timer is: " + timer + ", timer info is: " + timer.getInfo());
        timerInfo = (String) timer.getInfo();

        timerServiceCount++;
        latch.countDown();
    }

    public static int awaitTimerServiceCount() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCount;
    }

    public static int getTimerServiceCount() {
        return timerServiceCount;
    }
}
