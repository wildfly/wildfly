/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.serialization;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

/**
 * @author Stuart Douglas
 */
@Stateless
public class TimerServiceSerializationBean {

    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final int TIMER_INIT_TIME_MS = 100;
    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    // should be greater than (timer init time + timeout time)
    private static final int TIMER_CALL_WAITING_S = 30;

    private static volatile InfoA info;

    @Resource
    private TimerService timerService;

    public void createTimer() {
        timerService.createTimer(TIMER_INIT_TIME_MS, TIMER_TIMEOUT_TIME_MS, new InfoA());
    }

    @Timeout
    public void timeout(Timer timer) {
        info = (InfoA) timer.getInfo();
        latch.countDown();
    }

    public static InfoA awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return info;
    }

}
