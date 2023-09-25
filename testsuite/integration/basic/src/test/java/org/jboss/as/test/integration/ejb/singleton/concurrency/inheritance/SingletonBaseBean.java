/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.concurrency.inheritance;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.ejb.AccessTimeout;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;


public class SingletonBaseBean {

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 0)
    public void writeLockOverriddenByParent(CountDownLatch cont, CountDownLatch entered) throws InterruptedException {
        entered.countDown();
        cont.countDown();
        if (!cont.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Could not acquire lock within 2 seconds");
        }
    }

    @Lock(LockType.READ)
    @AccessTimeout(value = 0)
    public void readLockOverriddenByParent(CountDownLatch cont, CountDownLatch entered) throws InterruptedException {
        entered.countDown();
        cont.countDown();
        if (!cont.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Could not acquire lock within 2 seconds");
        }
    }

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 0)
    public void writeLock(CountDownLatch cont, CountDownLatch entered) throws InterruptedException {
        entered.countDown();
        cont.countDown();
        if (!cont.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Could not acquire lock within 2 seconds");
        }
    }

    @Lock(LockType.READ)
    @AccessTimeout(value = 0)
    public void readLock(CountDownLatch cont, CountDownLatch entered) throws InterruptedException {
        entered.countDown();
        cont.countDown();
        if (!cont.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Could not acquire lock within 2 seconds");
        }
    }

    @AccessTimeout(value = 0)
    public void impliedWriteLock(CountDownLatch cont, CountDownLatch entered) throws InterruptedException {
        entered.countDown();
        cont.countDown();
        if (!cont.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Could not acquire lock within 2 seconds");
        }
    }

}
