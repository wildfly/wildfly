/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.tx.timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;

import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Tomasz Adamski
 */
@Stateless
public class PrivateTxScheduleBean extends AbstractTxBean {
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile boolean timerServiceCalled = false;
    private static volatile int timeout = 0;

    @Schedule(second = "*", minute = "*", hour = "*", persistent = false, info = "info", timezone = "Europe/Prague")
    @TransactionTimeout(value = 5)
    private void timeout(Timer timer) {
        timeout = checkTimeoutValue();
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

    public static int getTimeout() {
        return timeout;
    }

}
