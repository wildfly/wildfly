/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;

/**
 * @author Ondrej Chaloupka
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AsyncBeanSynchronizeSingleton implements AsyncBeanSynchronizeSingletonRemote {
    private static volatile CountDownLatch latch = new CountDownLatch(1);
    private static volatile CountDownLatch latch2 = new CountDownLatch(1);

    public void reset() {
        latch = new CountDownLatch(1);
        latch2 = new CountDownLatch(1);
    }

    public void latchCountDown() {
        latch.countDown();
    }

    public void latch2CountDown() {
        latch2.countDown();
    }

    public void latchAwaitSeconds(int sec) throws InterruptedException {
        if (!latch.await(sec, TimeUnit.SECONDS)) {
            throw new RuntimeException("Await failed");
        }
    }

    public void latch2AwaitSeconds(int sec) throws InterruptedException {
        latch2.await(sec, TimeUnit.SECONDS);
    }
}
