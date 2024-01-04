/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.async;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Stateless
public class StatelessRemoteBean implements RemoteInterface, LocalInterface {

    public static volatile CountDownLatch doneLatch = new CountDownLatch(1);
    public static volatile CountDownLatch startLatch = new CountDownLatch(1);

    public static void reset() {
        doneLatch = new CountDownLatch(1);
        startLatch = new CountDownLatch(1);
    }

    @Asynchronous
    public void modifyArray(final String[] array) {
        try {
            if(!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation was not asynchronous");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        array[0] = "goodbye";
        doneLatch.countDown();
    }


    @Asynchronous
    public Future<String> hello() {
        return new AsyncResult<String>("hello");
    }

    @Override
    @Asynchronous
    public Future<Void> alwaysFail() throws AppException {
        throw new AppException("Intentionally thrown");
    }

    @Override
    @Asynchronous
    public void passByReference(final String[] array) {
        try {
            if(!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation was not asynchronous");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        array[0] = "goodbye";
        doneLatch.countDown();
    }
}
