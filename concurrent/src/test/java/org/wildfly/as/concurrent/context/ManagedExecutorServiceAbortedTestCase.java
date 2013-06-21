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

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;

/**
 *
 */
public class ManagedExecutorServiceAbortedTestCase extends AbstractManagedExecutorServiceTestCase {

    // execute

    @Test
    public void testExecuteRunnableAborted() throws Exception {
        TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        executor.execute(task);
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testExecuteManagedRunnableAborted() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        executor.execute(task);
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener managedTaskListener = task.getTestManagedTaskListener();
        managedTaskListener.assertListenerDone();
        managedTaskListener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution);
    }

    // submit

    @Test
    public void testSubmitRunnableAborted() throws Exception {
        TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            executor.submit(task).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitRunnableWithValueAborted() throws Exception {
        TestRunnable task = new TestRunnable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            executor.submit(task, new Object()).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitCallableAborted() throws Exception {
        TestCallable task = new TestCallable();
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            executor.submit(task).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitManagedRunnableAborted() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            executor.submit(task).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener managedTaskListener = task.getTestManagedTaskListener();
        managedTaskListener.assertListenerDone();
        managedTaskListener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution);
    }

    @Test
    public void testSubmitManagedRunnableWithValueAborted() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution.length);
        task.innerTask = new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        };
        task.assertContextIsNotSet();
        try {
            executor.submit(task, new Object()).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener managedTaskListener = task.getTestManagedTaskListener();
        managedTaskListener.assertListenerDone();
        managedTaskListener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution);
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
            executor.submit(task).get();
            fail("task execution should have failed due to runtime exception");
        } catch (ExecutionException e) {
            // expected
        }
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener managedTaskListener = task.getTestManagedTaskListener();
        managedTaskListener.assertListenerDone();
        managedTaskListener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForAbortedExecution);
    }

}
