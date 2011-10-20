/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.singleton.concurrency.inheritance;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


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
