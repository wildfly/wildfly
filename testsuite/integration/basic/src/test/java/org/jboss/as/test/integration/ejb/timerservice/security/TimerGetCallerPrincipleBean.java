/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.security;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TimedObject;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Stateless
public class TimerGetCallerPrincipleBean implements TimedObject {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile String principle = null;

    @Resource
    private TimerService timerService;

    @Resource
    private EJBContext ejbContext;

    public void createTimer() {
        timerService.createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    public static String awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return principle;
    }

    @Override
    public void ejbTimeout(final Timer timer) {
        principle = ejbContext.getCallerPrincipal().getName();
        latch.countDown();
    }
}
