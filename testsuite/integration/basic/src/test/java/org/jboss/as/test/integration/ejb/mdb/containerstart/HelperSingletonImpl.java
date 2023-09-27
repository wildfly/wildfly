/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.containerstart;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Singleton
@Startup
@Remote(HelperSingleton.class)
public class HelperSingletonImpl implements HelperSingleton {
    public static CyclicBarrier barrier = new CyclicBarrier(2);

    public int await(String where, long timeout, TimeUnit unit) throws BrokenBarrierException, TimeoutException, InterruptedException {
        return barrier.await(timeout, unit);
    }

    public void reset() {
        barrier.reset();
    }
}
