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

import org.junit.Test;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.fail;

/**
 *
 */
public class ManagedExecutorServiceImplCancellationTestCase extends AbstractManagedExecutorServiceImplTestCase {

    // submit

    @Test
    public void testSubmitRunnableCancellationDuringExecution() throws Exception {
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
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitRunnableWithValueCancellationDuringExecution() throws Exception {
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
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitCallableCancellationDuringExecution() throws Exception {
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
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitManagedRunnableCancellationDuringExecution() throws Exception {
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
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener managedTaskListener = task.getTestManagedTaskListener();
        managedTaskListener.assertListenerDone();
        managedTaskListener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);
    }

    @Test
    public void testSubmitManagedRunnableWithValueCancellationDuringExecution() throws Exception {
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
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener managedTaskListener = task.getTestManagedTaskListener();
        managedTaskListener.assertListenerDone();
        managedTaskListener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);
    }

    @Test
    public void testSubmitManagedCallableCancellationDuringExecution() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final TestManagedCallable task = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution.length);
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
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener managedTaskListener = task.getTestManagedTaskListener();
        managedTaskListener.assertListenerDone();
        managedTaskListener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForCancellationDuringExecution);
    }

}
