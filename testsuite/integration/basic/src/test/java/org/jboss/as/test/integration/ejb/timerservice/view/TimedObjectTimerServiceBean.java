/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.view;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TimedObject;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Stateless
@Local(LocalInterface.class)
public class TimedObjectTimerServiceBean implements TimedObject, LocalInterface {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile boolean timerServiceCalled = false;

    @Resource
    private TimerService timerService;

    public void createTimer() {
        timerService.createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

    @Override
    public void ejbTimeout(final Timer timer) {
        timerServiceCalled = true;
        latch.countDown();
    }
}
