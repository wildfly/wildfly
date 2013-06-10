/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.as.concurrent.tasklistener;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * @author Eduardo Martins
 */
public abstract class TestTaskListener implements TaskListener {

    enum CallbackType {aborted, cancelled, done, starting, submitted}

    public static final CallbackType[] expectedCallbacksForSingleExecutionNormal = {CallbackType.submitted, CallbackType.starting, CallbackType.done};
    public static final CallbackType[] expectedCallbacksForSingleExecutionAborted = {CallbackType.submitted, CallbackType.starting, CallbackType.aborted};
    public static final CallbackType[] expectedCallbacksForSingleExecutionCancelledBeforeExecution = {CallbackType.submitted, CallbackType.cancelled};
    public static final CallbackType[] expectedCallbacksForSingleExecutionCancelledDuringExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.cancelled};

    public static final CallbackType[] expectedCallbacksForTwoExecutionsAborted = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.aborted};
    public static final CallbackType[] expectedCallbacksForTwoExecutionsCancelledBeforeExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.cancelled};
    public static final CallbackType[] expectedCallbacksForTwoExecutionsCancelledDuringExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.cancelled};

    private final ArrayList<CallbackType> callbacks = new ArrayList<>();
    private final CountDownLatch done;
    protected AtomicInteger executions = new AtomicInteger(0);

    public TestTaskListener(int expectedCallbacks) {
        this.done = new CountDownLatch(expectedCallbacks);
    }

    public ArrayList<CallbackType> getCallbacks() {
        return callbacks;
    }

    public int getExecutions() {
        return executions.get();
    }

    private synchronized void addCallback(CallbackType callbackType) {
        callbacks.add(callbackType);
        if (done.getCount() == 0) {
            throw new RuntimeException();
        }
        done.countDown();
    }

    @Override
    public void taskSubmitted(Future<?> future) {
        addCallback(CallbackType.submitted);
        Assert.assertNotNull(future);
    }

    @Override
    public void taskStarting() {
        addCallback(CallbackType.starting);
    }

    @Override
    public void taskDone(Throwable exception) {
        if (exception == null) {
            addCallback(CallbackType.done);
        } else if (exception instanceof CancellationException) {
            addCallback(CallbackType.cancelled);
        } else {
            addCallback(CallbackType.aborted);
        }
    }

    protected void assertDone(int seconds) throws InterruptedException {
        if (!this.waitForExpectedCallbacks(seconds, TimeUnit.SECONDS)) {
            fail("Callbacks: " + callbacks);
        }
    }

    public boolean waitForExpectedCallbacks(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return done.await(timeout, timeUnit);
    }

    protected void assertExecutions(int expectedExecutions) throws InterruptedException {
        Assert.assertEquals(expectedExecutions, getExecutions());
    }

    protected void assertExpectedCallbacks(CallbackType[] expectedCallbacks) throws InterruptedException {
        List<CallbackType> listenerCallbacks = this.getCallbacks();
        Assert.assertEquals("Unexpected callbacks size. Listener callbacks: " + listenerCallbacks + ". Expected callbacks: " + Arrays.asList(expectedCallbacks), expectedCallbacks.length, listenerCallbacks.size());
        for (int i = 0; i < expectedCallbacks.length; i++) {
            Assert.assertEquals(expectedCallbacks[i], listenerCallbacks.get(i));
        }
    }
}
