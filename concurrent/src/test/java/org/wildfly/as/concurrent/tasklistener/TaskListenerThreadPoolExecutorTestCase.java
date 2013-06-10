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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.fail;

/**
 *
 */
public class TaskListenerThreadPoolExecutorTestCase {

    protected TaskListenerExecutorService executor;

    protected TaskListenerExecutorService newExecutor() {
        return TestTaskListenerExecutors.newExecutor();
    }

    @Before
    public void beforeTest() {
        executor = newExecutor();
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null);
        executor.submit(task).get();
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionNormal);
    }

    @Test
    public void testSubmitRunnableWithValue() throws Exception {
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null);
        executor.submit(task, new Object()).get();
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionNormal);
    }

    @Test
    public void testSubmitCallable() throws Exception {
        TestTaskListenerCallable task = new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null);
        executor.submit(task).get();
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionNormal);
    }

    @Test
    public void testSubmitRunnableAborted() throws Exception {
        final Runnable innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionAborted.length, innerTask);
        try {
            executor.submit(task).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionAborted);
    }

    @Test
    public void testSubmitRunnableWithValueAborted() throws Exception {
        final Runnable innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionAborted.length, innerTask);
        try {
            executor.submit(task, new Object()).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionAborted);
    }

    @Test
    public void testSubmitCallableAborted() throws Exception {
        final Callable<Object> innerTask = new Callable() {
            @Override
            public Object call() throws Exception {
                throw new RuntimeException();
            }
        };
        TestTaskListenerCallable task = new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionAborted.length, innerTask);
        try {
            executor.submit(task).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionAborted);
    }

    @Test
    public void testExecute() throws Exception {
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null);
        executor.execute(task);
        task.assertDone(10);
        task.assertExecutions(1);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionNormal);
    }

    @Test
    public void testExecuteAborted() throws Exception {
        final Runnable innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionAborted.length, innerTask);
        executor.execute(task);
        task.assertDone(10);
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionAborted);
    }

    @Test
    public void testSubmitRunnableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final Runnable innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionCancelledDuringExecution.length, innerTask);
        Future future = executor.submit(task);
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
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionCancelledDuringExecution);
    }

    @Test
    public void testSubmitRunnableWithValueCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final Runnable innerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        TestTaskListenerRunnable task = new TestTaskListenerRunnable(TestTaskListener.expectedCallbacksForSingleExecutionCancelledDuringExecution.length, innerTask);
        Future future = executor.submit(task, new Object());
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
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionCancelledDuringExecution);
    }

    @Test
    public void testSubmitCallableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final Callable<Object> innerTask = new Callable() {
            @Override
            public Object call() throws Exception {
                lock.lockInterruptibly();
                return new Object();
            }
        };
        TestTaskListenerCallable task = new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionCancelledDuringExecution.length, innerTask);
        Future future = executor.submit(task);
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
        task.assertExecutions(0);
        task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionCancelledDuringExecution);
    }

    @Test
    public void testInvokeAll() throws Exception {
        Set<TestTaskListenerCallable<Object>> taskSet = new HashSet<>();
        taskSet.add(new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null));
        taskSet.add(new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null));
        taskSet.add(new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null));
        List<Future<Object>> futures = executor.invokeAll(taskSet);
        for (Future future : futures) {
            future.get();
        }
        for (TestTaskListenerCallable task : taskSet) {
            task.assertDone(10);
            task.assertExecutions(1);
            task.assertExpectedCallbacks(TestTaskListener.expectedCallbacksForSingleExecutionNormal);
        }
    }

    @Test
    public void testInvokeAny() throws Exception {
        Set<TestTaskListenerCallable<Object>> taskSet = new HashSet<>();
        taskSet.add(new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null));
        taskSet.add(new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null));
        taskSet.add(new TestTaskListenerCallable(TestTaskListener.expectedCallbacksForSingleExecutionNormal.length, null));
        executor.invokeAny(taskSet);
    }

    @After
    public void afterTest() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            fail();
        }
    }

}
