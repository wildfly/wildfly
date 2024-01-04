/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.reentrant;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;

import org.jboss.logging.Logger;

@Singleton
public class SingletonBean {
    private static final Logger log = Logger.getLogger(SingletonBean.class);
    private static final int METHOD_WAIT_MS = 2000;
    private static final int AWAIT_TIME_S = 2;

    private Integer called = 0;

    @Resource
    SessionContext ctx;

    @Lock(LockType.WRITE)
    public Integer methodWithWriteLock(CountDownLatch downMe, CountDownLatch waitingForOther) throws Exception {
        log.trace("methodwith write lock is here!");
        downMe.countDown();
        if (waitingForOther != null) {
            waitingForOther.await(AWAIT_TIME_S, TimeUnit.SECONDS);
            SingletonBean bean = (SingletonBean) ctx.lookup("java:module/" + SingletonBean.class.getSimpleName());
            bean.reentrantRead();
            bean.reentrantWrite();
        }
        log.trace("methodWithWriteLock [" + called + "]");
        return new Integer(++called);
    }

    @Lock(LockType.READ)
    public Integer methodWithReadLock(CountDownLatch waitingForOther) throws Exception {
        waitingForOther.await(AWAIT_TIME_S, TimeUnit.SECONDS);
        SingletonBean bean = (SingletonBean) ctx.lookup("java:module/" + SingletonBean.class.getSimpleName());
        bean.reentrantRead();
        log.trace("methodWithReadLock [" + called + "]");
        return new Integer(++called);
    }

    @Lock(LockType.READ)
    public Integer methodWithReadLockException() throws Exception {
        SingletonBean bean = (SingletonBean) ctx.lookup("java:module/" + SingletonBean.class.getSimpleName());
        bean.reentrantWrite();
        log.trace("This should not occur [" + called + "]");
        return new Integer(++called);
    }

    @Lock(LockType.READ)
    public void reentrantRead() throws InterruptedException {
        // sleeping to other thread would have change for action
        Thread.sleep(METHOD_WAIT_MS);
    }

    @Lock(LockType.WRITE)
    public void reentrantWrite() throws InterruptedException {
        // sleeping to other thread would have change for action
        Thread.sleep(METHOD_WAIT_MS);
    }

    public void resetCalled() {
        called = 0;
    }

    public Integer getCalled() {
        return called;
    }
}
