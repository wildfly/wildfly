/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Singleton;

/**
 * Singleton
 */
@Singleton
@Asynchronous
public class AsyncSingleton {

    public static volatile boolean voidMethodCalled = false;
    public static volatile boolean futureMethodCalled = false;

    public void asyncMethod(CountDownLatch latch, CountDownLatch latch2) throws InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
        voidMethodCalled = true;
        latch2.countDown();
    }

    public Future<Boolean> futureMethod(CountDownLatch latch) throws InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
        futureMethodCalled = true;
        return new AsyncResult<Boolean>(true);
    }

}
