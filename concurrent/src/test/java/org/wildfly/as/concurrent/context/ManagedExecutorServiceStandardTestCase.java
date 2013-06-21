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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 *
 */
public class ManagedExecutorServiceStandardTestCase extends AbstractManagedExecutorServiceTestCase {

    // execute

    @Test
    public void testExecuteRunnable() throws Exception {
        TestRunnable task = new TestRunnable();
        task.assertContextIsNotSet();
        executor.execute(task);
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testExecuteManagedRunnable() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length, false);
        task.assertContextIsNotSet();
        executor.execute(task);
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);
    }

    // submit

    @Test
    public void testSubmitRunnable() throws Exception {
        TestRunnable task = new TestRunnable();
        task.assertContextIsNotSet();
        executor.submit(task).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitRunnableWithValue() throws Exception {
        TestRunnable task = new TestRunnable();
        task.assertContextIsNotSet();
        executor.submit(task, new Object()).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitCallable() throws Exception {
        TestCallable task = new TestCallable();
        task.assertContextIsNotSet();
        executor.submit(task).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testSubmitManagedRunnable() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length);
        task.assertContextIsNotSet();
        executor.submit(task).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);
    }

    @Test
    public void testSubmitManagedRunnableWithValue() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length, false);
        task.assertContextIsNotSet();
        executor.submit(task, new Object()).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);
    }

    @Test
    public void testSubmitManagedCallable() throws Exception {
        TestManagedCallable task = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length);
        task.assertContextIsNotSet();
        executor.submit(task).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);
    }

    // invoke all

    @Test
    public void testInvokeAllCallable() throws Exception {
        Set<TestCallable<Object>> taskSet = new HashSet<>();
        taskSet.add(new TestCallable());
        taskSet.add(new TestCallable());
        taskSet.add(new TestCallable());
        for (TestCallable task : taskSet) {
            task.assertContextIsNotSet();
        }
        List<Future<Object>> futures = executor.invokeAll(taskSet);
        for (Future future : futures) {
            future.get();
        }
        for (TestCallable callable : taskSet) {
            callable.assertContextWasSet();
            callable.assertContextWasReset();
        }
    }

    @Test
    public void testInvokeAllManagedCallable() throws Exception {
        Set<TestManagedCallable<Object>> taskSet = new HashSet<>();
        taskSet.add(new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length, false));
        taskSet.add(new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length, true));
        taskSet.add(new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length, false));
        for (TestManagedCallable task : taskSet) {
            task.assertContextIsNotSet();
        }
        List<Future<Object>> futures = executor.invokeAll(taskSet);
        for (Future future : futures) {
            future.get();
        }
        for (TestManagedCallable managedCallable : taskSet) {
            managedCallable.assertContextWasSet();
            managedCallable.assertContextWasReset();
            TestManagedTaskListener listener = managedCallable.getTestManagedTaskListener();
            listener.assertListenerDone();
            listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);
        }
    }

    // invoke any 

    @Test
    public void testInvokeAnyCallable() throws Exception {
        Set<TestCallable<Object>> taskSet = new HashSet<>();
        taskSet.add(new TestCallable());
        taskSet.add(new TestCallable());
        taskSet.add(new TestCallable());
        for (TestCallable task : taskSet) {
            task.assertContextIsNotSet();
        }
        executor.invokeAny(taskSet);
        // TODO ensure any was truly invoked with success
    }

    @Test
    public void testInvokeAnyManagedCallable() throws Exception {
        Set<TestManagedCallable<Object>> taskSet = new HashSet<>();
        taskSet.add(new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length));
        taskSet.add(new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length));
        taskSet.add(new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length));
        for (TestManagedCallable task : taskSet) {
            task.assertContextIsNotSet();
        }
        executor.invokeAny(taskSet);
        // TODO ensure any was truly invoked with success
    }

}
