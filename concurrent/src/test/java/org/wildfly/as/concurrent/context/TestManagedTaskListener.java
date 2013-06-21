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

package org.wildfly.as.concurrent.context;

import org.junit.Assert;

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.SkippedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * @author Eduardo Martins
 */
public class TestManagedTaskListener implements ManagedTaskListener {

    public enum CallbackType {aborted, cancelled, skipped, done, starting, submitted, unexpected}

    public static final CallbackType[] expectedListenerCallbacksForNormalExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done};
    public static final CallbackType[] expectedListenerCallbacksForAbortedExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.aborted, CallbackType.done};
    public static final CallbackType[] expectedListenerCallbacksForCancellationBeforeExecution = {CallbackType.submitted, CallbackType.cancelled, CallbackType.done};
    public static final CallbackType[] expectedListenerCallbacksForCancellationDuringExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.cancelled, CallbackType.done};
    public static final CallbackType[] expectedListenerCallbacksForTriggerExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.done};
    public static final CallbackType[] expectedListenerCallbacksForTriggerWithSkipExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.skipped, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.done};
    public static final CallbackType[] expectedListenerCallbacksForTriggerWithAbortExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.aborted, CallbackType.done};
    public static final CallbackType[] expectedListenerCallbacksForTriggerWithSkipAndAbortExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.skipped, CallbackType.done, CallbackType.submitted, CallbackType.aborted, CallbackType.done};

    public static final CallbackType[] expectedCallbacksForTwoExecutionsAborted = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.aborted, CallbackType.done};
    public static final CallbackType[] expectedCallbacksForTwoExecutionsCancelledBeforeExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.cancelled, CallbackType.done};
    public static final CallbackType[] expectedCallbacksForTwoExecutionsCancelledDuringExecution = {CallbackType.submitted, CallbackType.starting, CallbackType.done, CallbackType.submitted, CallbackType.starting, CallbackType.cancelled, CallbackType.done};

    private final ArrayList<CallbackType> callbacks = new ArrayList<>();
    private final CountDownLatch done;
    private final boolean contextual;
    private boolean expectedContext = true;


    public TestManagedTaskListener(int expectedCallbacks, boolean contextual) {
        this.contextual = contextual;
        this.done = new CountDownLatch(expectedCallbacks);

    }

    public ArrayList<CallbackType> getCallbacks() {
        return callbacks;
    }

    public boolean waitForExpectedCallbacks(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return done.await(timeout, timeUnit);
    }

    private synchronized void addCallback(CallbackType callbackType) {
        if (expectedContext) {
            TestContext.SetData contextSetData = TestContext.getCurrent();
            if (contextual) {
                expectedContext = contextSetData != null && contextSetData.testContext.getObject() == this;
            } else {
                expectedContext = contextSetData == null || contextSetData.testContext.getObject() != this;
            }
            if (!expectedContext) {
                throw new RuntimeException("unexpected context for " + contextSetData + " set on " + (contextual ? "" : "non") + " contextual listener callback " + callbackType);
            }
        }
        callbacks.add(callbackType);
        if (done.getCount() == 0) {
            throw new RuntimeException();
        }
        done.countDown();
    }

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Throwable exception) {
        if (exception instanceof AbortedException) {
            addCallback(CallbackType.aborted);
        } else if (exception instanceof CancellationException) {
            addCallback(CallbackType.cancelled);
        } else if (exception instanceof SkippedException) {
            addCallback(CallbackType.skipped);
        } else {
            addCallback(CallbackType.unexpected);
        }
    }

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Throwable exception) {
        addCallback(CallbackType.done);
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor) {
        addCallback(CallbackType.starting);
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor) {
        addCallback(CallbackType.submitted);
    }

    public void assertListenerDone() throws InterruptedException {
        assertListenerDone(5);
    }

    public void assertListenerDone(int seconds) throws InterruptedException {
        if (!this.waitForExpectedCallbacks(seconds, TimeUnit.SECONDS)) {
            fail("Callbacks: " + callbacks);
        }
        assertExpectedContextOnCallbacks();
    }

    public void assertListenerGotExpectedCallbacks(CallbackType[] expectedCallbacks) throws InterruptedException {
        assertListenerGotExpectedCallbacks(expectedCallbacks, true);
    }

    public void assertListenerGotExpectedCallbacks(CallbackType[] expectedCallbacks, boolean assertSameLength) throws InterruptedException {
        List<CallbackType> listenerCallbacks = this.getCallbacks();
        if (assertSameLength) {
            Assert.assertEquals("Unexpected listener callbacks size. Listener callbacks: " + listenerCallbacks + ". Expected callbacks: " + Arrays.asList(expectedCallbacks), expectedCallbacks.length, listenerCallbacks.size());
        }
        for (int i = 0; i < expectedCallbacks.length; i++) {
            Assert.assertEquals(expectedCallbacks[i], listenerCallbacks.get(i));
        }
    }

    public void assertExpectedContextOnCallbacks() {
        Assert.assertTrue(expectedContext);
    }
}
