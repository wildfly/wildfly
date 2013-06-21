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


import org.junit.Assert;
import org.junit.Test;
import org.wildfly.as.concurrent.component.ComponentManagedExecutorService;
import org.wildfly.as.concurrent.component.ComponentManagedScheduledExecutorService;
import org.wildfly.as.concurrent.context.TestCallable;
import org.wildfly.as.concurrent.context.TestManagedCallable;
import org.wildfly.as.concurrent.context.TestManagedRunnable;
import org.wildfly.as.concurrent.context.TestManagedTaskListener;
import org.wildfly.as.concurrent.context.TestRunnable;
import org.wildfly.as.concurrent.tasklistener.TestTaskListenerExecutors;
import org.wildfly.as.concurrent.tasklistener.impl.TaskListenerScheduledThreadPoolExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class SharedTaskListenerExecutorServiceTestCase extends SharedManagedTaskThreadPoolExecutorTestCase {

    protected ComponentManagedScheduledExecutorService componentScheduler1;
    protected ComponentManagedScheduledExecutorService componentScheduler2;
    protected ComponentManagedScheduledExecutorService componentScheduler3;

    protected void createExecutors() {
        super.createExecutors();
        componentScheduler1 = (ComponentManagedScheduledExecutorService) componentExecutor1;
        componentScheduler2 = (ComponentManagedScheduledExecutorService) componentExecutor2;
    }

    protected void createThreadPoolExecutor() {
        taskListenerExecutorService = TestTaskListenerExecutors.newScheduledExecutor();
    }

    protected ComponentManagedExecutorService createServerManagedExecutorService() {
        return TestExecutors.newComponentManagedScheduledExecutorService((TaskListenerScheduledThreadPoolExecutor) taskListenerExecutorService);
    }

    @Test
    public void testScheduleThreadPoolExecutorSharing() throws Exception {

        // schedule a few tasks to executor 1, some which blocks on execution, other with long delay for execution
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
        Future future11 = componentScheduler1.schedule(task11, 10, TimeUnit.NANOSECONDS);
        while (!lock11.hasQueuedThreads()) {
            Thread.sleep(10);
        }

        TestCallable task12 = new TestCallable();
        final Future future12 = componentScheduler1.schedule(task12, 10, TimeUnit.HOURS);


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
        final Future future13 = componentScheduler1.schedule(task13, 10, TimeUnit.NANOSECONDS);
        // wait till the task is blocked
        while (!lock13.hasQueuedThreads()) {
            Thread.sleep(10);
        }

        // schedule another task that blocks on execution, but to executor 2
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
        final Future future2 = componentScheduler2.schedule(task2, 10, TimeUnit.NANOSECONDS);
        // wait till the task is blocked
        while (!lock2.hasQueuedThreads()) {
            Thread.sleep(10);
        }

        // shutdown executor 1, this should cancel all 1x tasks
        shutdownServerManagedExecutorService(componentScheduler1);

        // assert tasks 1x status
        Assert.assertTrue(future11.isCancelled());
        Assert.assertTrue(future11.isDone());
        task11.assertContextWasSet();
        task11.assertContextWasReset();
        Assert.assertTrue(future12.isCancelled());
        Assert.assertTrue(future12.isDone());
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
        shutdownServerManagedExecutorService(componentScheduler2);

        // ensure shared thread pool is alive
        Assert.assertFalse(taskListenerExecutorService.isShutdown());

        // create a 3rd executor and execute a task
        componentScheduler3 = (ComponentManagedScheduledExecutorService) createServerManagedExecutorService();
        final TestRunnable task3 = new TestRunnable();
        componentScheduler3.schedule(task3, 10, TimeUnit.NANOSECONDS).get();
        task3.assertContextWasSet();
        task3.assertContextWasReset();
    }

}
