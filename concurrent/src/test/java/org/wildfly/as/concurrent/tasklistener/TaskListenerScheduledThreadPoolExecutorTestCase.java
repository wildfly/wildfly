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

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.fail;

/**
 *
 */
public class TaskListenerScheduledThreadPoolExecutorTestCase extends TaskListenerThreadPoolExecutorTestCase {

    protected TaskListenerScheduledExecutorService scheduledExecutor;

    protected TaskListenerExecutorService newExecutor() {
        return TestTaskListenerExecutors.newScheduledExecutor();
    }

    @Before
    public void beforeTest() {
        super.beforeTest();
        scheduledExecutor = (TaskListenerScheduledExecutorService) executor;
    }

    @Test
    public void testScheduleRunnable() throws Exception {
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null);
        scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionNormal);
    }

    @Test
    public void testScheduleCallable() throws Exception {
        TestTaskListenerCallable task = new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null);
        scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionNormal);
    }

    @Test
    public void testScheduleRunnableAborted() throws Exception {
        final Runnable innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionAborted.length, innerTask);
        try {
            scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionAborted);
    }

    @Test
    public void testScheduleCallableAborted() throws Exception {
        final Callable<Object> innerTask = new Callable() {
            @Override
            public Object call() throws Exception {
                throw new RuntimeException();
            }
        };
        TestTaskListenerCallable task = new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionAborted.length, innerTask);
        try {
            scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionAborted);
    }

    @Test
    public void testScheduleRunnableCancellationBeforeExecution() throws Exception {
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionCancelledBeforeExecution.length, null);
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.SECONDS);
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionCancelledBeforeExecution);
    }

    @Test
    public void testScheduleCallableCancellationBeforeExecution() throws Exception {
        TestTaskListenerCallable task = new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionCancelledBeforeExecution.length, null);
        Future future = scheduledExecutor.schedule(task, 10, TimeUnit.SECONDS);
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionCancelledBeforeExecution);
    }

    @Test
    public void testScheduleAtFixedRateAborted() throws Exception {
        final Runnable innerTask = new Runnable() {
            int executions = 0;

            @Override
            public void run() {
                if (executions > 0) {
                    throw new RuntimeException();
                }
                executions++;
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForTwoExecutionsAborted.length, innerTask);
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10, TimeUnit.MILLISECONDS);
        while (task.getExecutions() < 1) {
            Thread.sleep(50);
        }
        try {
            future.get();
            fail("task should have been aborted");
        } catch (Throwable e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForTwoExecutionsAborted);

    }

    @Test
    public void testScheduleAtFixedRateCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final Runnable innerTask = new Runnable() {
            int executions = 0;

            @Override
            public void run() {
                if (executions > 0) {
                    try {
                        lock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                executions++;
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution.length, innerTask);
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
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution);
    }

    @Test
    public void testScheduleAtFixedRateCancellationBeforeExecution() throws Exception {
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution.length, null);
        Future future = scheduledExecutor.scheduleAtFixedRate(task, 10, 10000, TimeUnit.MILLISECONDS);
        while (task.getExecutions() != 1) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution);
    }

    @Test
    public void testScheduleWithFixedDelayAborted() throws Exception {
        final Runnable innerTask = new Runnable() {
            int executions = 0;

            @Override
            public void run() {
                if (executions > 0) {
                    throw new RuntimeException();
                }
                executions++;
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForTwoExecutionsAborted.length, innerTask);
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10, TimeUnit.MILLISECONDS);
        while (task.getExecutions() < 1) {
            Thread.sleep(50);
        }
        try {
            future.get();
            fail("task should have been aborted");
        } catch (Throwable e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForTwoExecutionsAborted);

    }

    @Test
    public void testScheduleWithFixedDelayCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final Runnable innerTask = new Runnable() {
            int executions = 0;

            @Override
            public void run() {
                if (executions > 0) {
                    try {
                        lock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                executions++;
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution.length, innerTask);
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
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledDuringExecution);
    }

    @Test
    public void testScheduleWithFixedDelayCancellationBeforeExecution() throws Exception {
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution.length, null);
        Future future = scheduledExecutor.scheduleWithFixedDelay(task, 10, 10000, TimeUnit.MILLISECONDS);
        while (task.getExecutions() != 1) {
            Thread.sleep(10);
        }
        future.cancel(true);
        try {
            future.get();
            fail("task should have been cancelled");
        } catch (Throwable e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForTwoExecutionsCancelledBeforeExecution);
    }

}
