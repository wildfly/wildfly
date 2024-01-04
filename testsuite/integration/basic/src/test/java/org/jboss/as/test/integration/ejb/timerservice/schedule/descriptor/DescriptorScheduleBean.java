/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.schedule.descriptor;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ejb.Timer;

/**
 * Ejb with it's timers managed by a descriptor
 *
 * @author Stuart Douglas
 */
public class DescriptorScheduleBean {

    private static volatile String timerInfo;
    private static volatile Date start;

    private static int TIMER_CALL_WAITING_S = 30;
    private static volatile CountDownLatch latch = new CountDownLatch(1);

    public void descriptorScheduledMethod(final Timer timer) {
        timerInfo = (String) timer.getInfo();
        start = timer.getSchedule().getStart();
        latch.countDown();
    }

    public static boolean awaitTimer(){
        try {
            final boolean success = latch.await(TIMER_CALL_WAITING_S, TimeUnit.SECONDS);
            if (!success)
                throw new IllegalStateException("Timeout method was not called");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerInfo != null;
    }

    public static Date getStart() {
        return start;
    }

    public static String getTimerInfo() {
        return timerInfo;
    }
}
