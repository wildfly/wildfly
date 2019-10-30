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

package org.jboss.as.test.integration.ejb.singleton.reentrant;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

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
