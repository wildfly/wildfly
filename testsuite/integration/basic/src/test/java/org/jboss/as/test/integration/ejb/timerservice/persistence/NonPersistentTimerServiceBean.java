/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.persistence;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

/**
 * @author Ondrej Chaloupka
 */
@Singleton
public class NonPersistentTimerServiceBean {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_CALL_QUICK_WAITING_S = 2;

    private static volatile boolean timerServiceCalled = false;

    @Resource
    private TimerService timerService;

    public void createTimer() {
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createIntervalTimer(100, 100, timerConfig);
    }

    @Timeout
    public void timeout() {
        timerServiceCalled = true;
        latch.countDown();
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
