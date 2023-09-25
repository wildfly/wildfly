/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.cdi.requestscope;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerService;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateless
public class AnnotationTimerServiceBean {

    private static final int TIMER_TIMEOUT_TIME_MS = 100;
    // has to be greater than timeout time
    private static final int TIMER_CALL_WAITING_MS = 30000;
    private static final CountDownLatch latch = new CountDownLatch(1);

    private static volatile String cdiMessage = null;

    @Inject
    private CdiBean cdiBean;

    @Resource
    private TimerService timerService;

    public void createTimer() {
        timerService.createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    @Timeout
    public void timeout() {
        cdiMessage = cdiBean.getMessage();
        latch.countDown();
    }

    public static String awaitTimerCall() {
        try {
            latch.await(TIMER_CALL_WAITING_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return cdiMessage;
    }

}
