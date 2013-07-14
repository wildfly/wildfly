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

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.fail;

/**
 *
 */
public class ManagedScheduledExecutorServiceImplCancellationTestCase extends ManagedExecutorServiceImplCancellationTestCase {

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
    public void testScheduleRunnableCancellationBeforeExecution() throws Exception {
        TestRunnable task = new TestRunnable();
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.SECONDS);
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextIsNotSet();
    }

    @Test
    public void testScheduleCallableCancellationBeforeExecution() throws Exception {
        TestCallable task = new TestCallable();
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.SECONDS);
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextIsNotSet();
    }

    @Test
    public void testScheduleManagedRunnableCancellationBeforeExecution() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution.length);
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.SECONDS);
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextIsNotSet();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution);
    }

    @Test
    public void testScheduleManagedCallableCancellationBeforeExecution() throws Exception {
        TestManagedCallable task = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution.length);
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.SECONDS);
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextIsNotSet();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution);
    }

    @Test
    public void testScheduleRunnableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }


    @Test
    public void testScheduleCallableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestCallable task = new TestCallable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleManagedRunnableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);
    }

    @Test
    public void testScheduleManagedRunnableWithValueCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task.assertContextIsNotSet();
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);
    }

    // periodic - at fixed rate

    @Test
    public void testScheduleAtFixedRateRunnableCancellationDuringExecution() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    try {
                        lock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleAtFixedRateManagedRunnableCancellationDuringExecution() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    try {
                        lock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution);
    }

    @Test
    public void testScheduleAtFixedRateRunnableCancellationBeforeExecution() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                done.countDown();
            }
        };
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10000, TimeUnit.MILLISECONDS);
        if (!done.await(1, TimeUnit.SECONDS)) {
            fail("task should have been executed");
        }
        // give it time to reschedule
        Thread.sleep(100);
        // and cancel
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleAtFixedRateManagedRunnableCancellationBeforeExecution() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                done.countDown();
            }
        };
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10000, TimeUnit.MILLISECONDS);
        if (!done.await(1, TimeUnit.SECONDS)) {
            fail("task should have been executed");
        }
        // give it time to reschedule
        Thread.sleep(100);
        // and cancel
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution);
    }

    // periodic - with fixed delay

    @Test
    public void testScheduleWithFixedDelayRunnableCancellationDuringExecution() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    try {
                        lock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleWithFixedDelayManagedRunnableCancellationDuringExecution() throws Exception {
        final AtomicInteger executions = new AtomicInteger(0);
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                if (executions.incrementAndGet() > 1) {
                    try {
                        lock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10, TimeUnit.MILLISECONDS);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution);
    }

    @Test
    public void testScheduleWithFixedDelayRunnableCancellationBeforeExecution() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                done.countDown();
            }
        };
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10000, TimeUnit.MILLISECONDS);
        if (!done.await(1, TimeUnit.SECONDS)) {
            fail("task should have been executed");
        }
        // give it time to reschedule
        Thread.sleep(100);
        // and cancel
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleWithFixedDelayManagedRunnableCancellationBeforeExecution() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                done.countDown();
            }
        };
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10000, TimeUnit.MILLISECONDS);
        if (!done.await(1, TimeUnit.SECONDS)) {
            fail("task should have been executed");
        }
        // give it time to reschedule
        Thread.sleep(100);
        // and cancel
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution);
    }

    // trigger

    @Test
    public void testScheduleTriggerRunnableCancellationBeforeExecution() throws Exception {
        TestRunnable task = new TestRunnable();
        task.assertContextIsNotSet();
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 5000L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        scheduledExecutor.schedule(task, trigger).cancel(false);
        task.assertContextIsNotSet();
    }

    @Test
    public void testScheduleTriggerCallableCancellationBeforeExecution() throws Exception {
        TestCallable task = new TestCallable();
        task.assertContextIsNotSet();
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 5000L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        scheduledExecutor.schedule(task, trigger).cancel(false);
        task.assertContextIsNotSet();
    }

    @Test
    public void testScheduleTriggerManagedRunnableCancellationBeforeExecution() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution.length);
        task.assertContextIsNotSet();
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 5000L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        scheduledExecutor.schedule(task, trigger).cancel(false);
        task.assertContextIsNotSet();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution);
    }

    @Test
    public void testScheduleTriggerManagedCallableCancellationBeforeExecution() throws Exception {
        TestManagedCallable task = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution.length);
        task.assertContextIsNotSet();
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 5000L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        scheduledExecutor.schedule(task, trigger).cancel(false);
        task.assertContextIsNotSet();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationBeforeExecution);
    }

    @Test
    public void testScheduleTriggerRunnableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        TestRunnable task = new TestRunnable();
        task.assertContextIsNotSet();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // create trigger to schedule task for 10ms delay
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 10L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        Future future = scheduledExecutor.schedule(task, trigger);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleTriggerCallableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        TestCallable task = new TestCallable();
        task.assertContextIsNotSet();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // create trigger to schedule task for 10ms delay
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 10L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        Future future = scheduledExecutor.schedule(task, trigger);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleTriggerManagedRunnableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution.length);
        task.assertContextIsNotSet();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // create trigger to schedule task for 10ms delay
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 10L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        Future future = scheduledExecutor.schedule(task, trigger);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);
    }

    @Test
    public void testScheduleTriggerManagedCallableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        TestManagedCallable task = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution.length);
        task.assertContextIsNotSet();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // create trigger to schedule task for 10ms delay
        final Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
                return new Date(System.currentTimeMillis() + 10L);
            }

            @Override
            public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
                return false;
            }
        };
        Future future = scheduledExecutor.schedule(task, trigger);
        while (!lock.hasQueuedThreads()) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);
    }

}
