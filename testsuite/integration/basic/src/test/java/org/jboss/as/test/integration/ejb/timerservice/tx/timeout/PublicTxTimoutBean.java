/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.tx.timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Tomasz Adamski
 */

@Stateless
@Remote(TimeoutBeanRemoteView.class)
public class PublicTxTimoutBean extends AbstractTxBean implements TimeoutBeanRemoteView {

    private static final int TIMER_CALL_WAITING_S = 30;
    private static final int DURATION = 100;

    private static volatile CountDownLatch latch = new CountDownLatch(1);

    private static volatile boolean timerServiceCalled = false;
    private static volatile int timeout = 0;

    @Resource
    private SessionContext sessionContext;

    @Resource
    private TimerService timerService;

    @Override
    public void startTimer() {
        timerService.createSingleActionTimer(DURATION, new TimerConfig());
    }

    @Timeout
    @TransactionTimeout(value = 5)
    public void timeout(final Timer timer) {
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

    public static int getTimeout(){
        return timeout;
    }


}
