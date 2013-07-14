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

package org.wildfly.ee.concurrent;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 *
 */
public class ManagedScheduledExecutorServiceImplAbortedTestCase extends ManagedExecutorServiceImplAbortedTestCase {

    protected ManagedScheduledExecutorServiceImpl scheduledExecutor;

    @Override
    protected ManagedExecutorServiceImpl newExecutor() {
        return TestExecutors.newManagedScheduledExecutorService();
    }

    @Before
    public void beforeTest() {
        super.beforeTest();
        scheduledExecutor = (ManagedScheduledExecutorServiceImpl) executor;
    }

    // schedule

    @Test
    public void testScheduleRunnableAborted() throws Exception {
        TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleCallableAborted() throws Exception {
        TestCallable task = new TestCallable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleManagedRunnableAborted() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution);
    }

    @Test
    public void testSubmitManagedCallableAborted() throws Exception {
        TestManagedCallable task = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution);
    }

    // periodic - at fixed rate

    @Test
    public void testScheduleAtFixedRateRunnableAborted() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    throw new RuntimeException();
                }
            }
        };
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);
        while (executions.get() < 1) {
            Thread.sleep(50);
        }
        try {
            future.get();
            fail("task should have been aborted");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleAtFixedRateManagedRunnableAborted() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedCallbacksForTwoExecutionsAborted.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    throw new RuntimeException();
                }
            }
        };
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);
        while (executions.get() < 1) {
            Thread.sleep(50);
        }
        try {
            future.get();
            fail("task should have been aborted");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedCallbacksForTwoExecutionsAborted);
    }

    // periodic - with fixed delay

    @Test
    public void testScheduleWithFixedDelayRunnableAborted() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    throw new RuntimeException();
                }
            }
        };
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10, TimeUnit.MILLISECONDS);
        while (executions.get() < 1) {
            Thread.sleep(50);
        }
        try {
            future.get();
            fail("task should have been aborted");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleWithFixedDelayManagedRunnableAborted() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedCallbacksForTwoExecutionsAborted.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    throw new RuntimeException();
                }
            }
        };
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10, TimeUnit.MILLISECONDS);
        while (executions.get() < 1) {
            Thread.sleep(50);
        }
        try {
            future.get();
            fail("task should have been aborted");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedCallbacksForTwoExecutionsAborted);
    }

    // trigger

    @Test
    public void testScheduleTriggerRunnableAborted() throws Exception {
        TestRunnable task = new TestRunnable(2);
        TestTrigger trigger = new TestTrigger(task.toString(), false, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerCallableAborted() throws Exception {
        TestCallable task = new TestCallable(2);
        TestTrigger trigger = new TestTrigger(task.toString(), false, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedRunnableAborted() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(2, TestManagedTaskListener.expectedListenerCallbacksForTriggerWithAbortExecution.length);
        TestTrigger trigger = new TestTrigger(task.toString(), false, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerWithAbortExecution);
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedCallableAborted() throws Exception {
        TestManagedCallable task = new TestManagedCallable(2, TestManagedTaskListener.expectedListenerCallbacksForTriggerWithAbortExecution.length);
        task.identityName = UUID.randomUUID().toString();
        TestTrigger trigger = new TestTrigger(task.identityName, false, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerWithAbortExecution);
        trigger.assertResults();
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerRunnableSkippedAndAborted() throws Exception {
        TestRunnable task = new TestRunnable(1);
        TestTrigger trigger = new TestTrigger(task.toString(), true, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerCallableSkippedAndAborted() throws Exception {
        TestCallable task = new TestCallable(1);
        TestTrigger trigger = new TestTrigger(task.toString(), true, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedRunnableSkippedAndAborted() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(1, TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipAndAbortExecution.length);
        task.identityName = UUID.randomUUID().toString();
        TestTrigger trigger = new TestTrigger(task.identityName, true, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipAndAbortExecution);
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedCallableSkippedAndAborted() throws Exception {
        TestManagedCallable task = new TestManagedCallable(1, TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipAndAbortExecution.length);
        TestTrigger trigger = new TestTrigger(task.toString(), true, true);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipAndAbortExecution);
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            // expected
        }
        assertFutureTermination(future, false);
    }

}
