/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;


/**
 * Stateless session bean invoked asynchronously.
 */
@Stateless
@Asynchronous
@LocalBean
public class AsyncBean implements AsyncBeanCancelRemoteInterface {
    public static volatile boolean voidMethodCalled = false;
    public static volatile boolean futureMethodCalled = false;

    @Inject
    private RequestScopedBean requestScopedBean;

    @Resource
    SessionContext ctx;

    @EJB
    AsyncBeanSynchronizeSingletonRemote synchronizeBean;

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

    public Future<Integer> testRequestScopeActive(CountDownLatch latch) throws InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
        requestScopedBean.setState(20);
        return new AsyncResult<Integer>(requestScopedBean.getState());
    }

    public Future<String> asyncCancelMethod(CountDownLatch latch, CountDownLatch latch2) throws InterruptedException {
        String result;
        result = ctx.wasCancelCalled() ? "true" : "false";

        latch.countDown();
        latch2.await(5, TimeUnit.SECONDS);

        result += ";";
        result += ctx.wasCancelCalled() ? "true" : "false";
        return new AsyncResult<String>(result);
    }

    public Future<String> asyncRemoteCancelMethod() throws InterruptedException {
        String result;
        result = ctx.wasCancelCalled() ? "true" : "false";

        synchronizeBean.latchCountDown();
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
            if (ctx.wasCancelCalled()) {
                break;
            }
            Thread.sleep(50);
        }
        result += ";";

        result += ctx.wasCancelCalled() ? "true" : "false";
        return new AsyncResult<String>(result);
    }

    public Future<String> asyncMethodWithException(boolean isException) {
        if (isException) {
            throw new IllegalArgumentException(); //some exception is thrown
        }
        return new AsyncResult<String>("Hi");
    }
}
