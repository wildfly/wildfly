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

package org.wildfly.as.concurrent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.as.concurrent.component.ComponentManagedExecutorService;
import org.wildfly.as.concurrent.context.TestCallable;
import org.wildfly.as.concurrent.context.TestManagedCallable;
import org.wildfly.as.concurrent.context.TestManagedRunnable;
import org.wildfly.as.concurrent.context.TestManagedTaskListener;
import org.wildfly.as.concurrent.context.TestRunnable;
import org.wildfly.as.concurrent.tasklistener.TaskListenerExecutorService;
import org.wildfly.as.concurrent.tasklistener.TestTaskListenerExecutors;
import org.wildfly.as.concurrent.tasklistener.impl.TaskListenerThreadPoolExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.fail;

/**
 *
 */
public class SharedManagedTaskThreadPoolExecutorTestCase {

    protected ComponentManagedExecutorService componentExecutor1;
    protected ComponentManagedExecutorService componentExecutor2;
    protected ComponentManagedExecutorService componentExecutor3;
    protected TaskListenerExecutorService taskListenerExecutorService;

    protected void createExecutors() {
        createThreadPoolExecutor();
        componentExecutor1 = createServerManagedExecutorService();
        componentExecutor2 = createServerManagedExecutorService();
    }

    protected void createThreadPoolExecutor() {
        taskListenerExecutorService = TestTaskListenerExecutors.newExecutor();
    }

    protected ComponentManagedExecutorService createServerManagedExecutorService() {
        return TestExecutors.newComponentManagedExecutorService((TaskListenerThreadPoolExecutor) taskListenerExecutorService);
    }

    @Before
    public void beforeTest() {
        createExecutors();
    }

    @Test
    public void testSubmitThreadPoolExecutorSharing() throws Exception {

        // submit a few tasks which blocks on execution to executor 1
        final ReentrantLock lock11 = new ReentrantLock();
        lock11.lock();
        final TestRunnable task11 = new TestRunnable();
        final Runnable innerTask11 = new Runnable() {
            @Override
            public void run() {
                try {
                    lock11.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task11.setInnerTask(innerTask11);
        Future future11 = componentExecutor1.submit(task11);
        while (!lock11.hasQueuedThreads()) {
            Thread.sleep(10);
        }

        final ReentrantLock lock12 = new ReentrantLock();
        lock12.lock();
        TestCallable task12 = new TestCallable();
        Runnable innerTask12 = new Runnable() {
            @Override
            public void run() {
                try {
                    lock12.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task12.setInnerTask(innerTask12);
        final Future future12 = componentExecutor1.submit(task12);
        while (!lock12.hasQueuedThreads()) {
            Thread.sleep(10);
        }

        final ReentrantLock lock13 = new ReentrantLock();
        lock13.lock();
        final TestManagedRunnable task13 = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution.length);
        final Runnable innerTask13 = new Runnable() {
            @Override
            public void run() {
                try {
                    lock13.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task13.setInnerTask(innerTask13);
        final Future future13 = componentExecutor1.submit(task13);
        // wait till the task is blocked
        while (!lock13.hasQueuedThreads()) {
            Thread.sleep(10);
        }

        // submit another task that blocks on execution, but to executor 2
        final ReentrantLock lock2 = new ReentrantLock();
        lock2.lock();
        final TestManagedCallable task2 = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length);
        final Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                try {
                    lock2.lockInterruptibly();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task2.setInnerTask(runnable2);
        final Future future2 = componentExecutor2.submit(task2);
        // wait till the task is blocked
        while (!lock2.hasQueuedThreads()) {
            Thread.sleep(10);
        }

        // shutdown executor 1, this should cancel all 1x tasks
        shutdownServerManagedExecutorService(componentExecutor1);

        // assert tasks 1x status
        Assert.assertTrue(future11.isCancelled());
        Assert.assertTrue(future11.isDone());
        task11.assertContextWasSet();
        task11.assertContextWasReset();
        Assert.assertTrue(future12.isCancelled());
        Assert.assertTrue(future12.isDone());
        task12.assertContextWasSet();
        task12.assertContextWasReset();
        Assert.assertTrue(future13.isCancelled());
        Assert.assertTrue(future13.isDone());
        task13.assertContextWasSet();
        task13.assertContextWasReset();
        final TestManagedTaskListener listener13 = task13.getTestManagedTaskListener();
        listener13.assertListenerDone();
        listener13.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);

        // ensure task 2 was not cancelled
        Assert.assertFalse(future2.isCancelled());
        Assert.assertFalse(future2.isDone());

        // unlock task2 so it completes its execution
        lock2.unlock();
        future2.get();
        task2.assertContextWasSet();
        task2.assertContextWasReset();
        final TestManagedTaskListener listener2 = task2.getTestManagedTaskListener();
        listener2.assertListenerDone();
        listener2.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);

        // shutdown executor 2
        shutdownServerManagedExecutorService(componentExecutor2);

        // ensure shared thread pool is alive
        Assert.assertFalse(taskListenerExecutorService.isShutdown());

        // create a 3rd executor and execute a task
        componentExecutor3 = createServerManagedExecutorService();
        final TestRunnable task3 = new TestRunnable();
        componentExecutor3.submit(task3).get();
        task3.assertContextWasSet();
        task3.assertContextWasReset();
    }

    @After
    public void afterTest() throws Exception {
        shutdownExecutors();
    }

    protected void shutdownExecutors() throws Exception {
        shutdownServerManagedExecutorService(componentExecutor1);
        shutdownServerManagedExecutorService(componentExecutor2);
        shutdownServerManagedExecutorService(componentExecutor3);
        shutdownThreadPoolExecutor();
    }

    protected void shutdownServerManagedExecutorService(ComponentManagedExecutorService executorService) throws Exception {
        if (executorService != null) {
            executorService.internalShutdown();
        }
    }

    protected void shutdownThreadPoolExecutor() throws Exception {
        if (taskListenerExecutorService != null) {
            taskListenerExecutorService.shutdown();
            if (!taskListenerExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                fail();
            }
        }
    }

}
