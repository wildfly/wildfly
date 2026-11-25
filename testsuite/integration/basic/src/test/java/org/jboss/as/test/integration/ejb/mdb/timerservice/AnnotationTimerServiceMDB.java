/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.timerservice;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerService;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

/**
 * @author Stuart Douglas
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/myAwesomeTopic")
})
public class AnnotationTimerServiceMDB implements MessageListener {

    private static final CountDownLatch latch = new CountDownLatch(1);

    private static boolean timerServiceCalled = false;

    @Resource
    private TimerService timerService;

    @Timeout
    public void timeout() {
        timerServiceCalled = true;
        latch.countDown();
    }

    public static boolean awaitTimerCall(int timeout) {
        try {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerServiceCalled;
    }

    @Override
    public void onMessage(final Message message) {
        timerService.createTimer(100, null);

    }
}
