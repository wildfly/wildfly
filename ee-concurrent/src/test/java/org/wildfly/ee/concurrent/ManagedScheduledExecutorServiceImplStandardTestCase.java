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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ManagedScheduledExecutorServiceImplStandardTestCase extends ManagedExecutorServiceImplStandardTestCase {

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
    public void testScheduleRunnable() throws Exception {
        TestRunnable task = new TestRunnable();
        scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleCallable() throws Exception {
        TestCallable task = new TestCallable();
        scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
    }

    @Test
    public void testScheduleManagedRunnable() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length);
        scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);
    }

    @Test
    public void testScheduleManagedCallable() throws Exception {
        TestManagedCallable task = new TestManagedCallable(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution.length);
        scheduledExecutor.schedule(task, 10, TimeUnit.MILLISECONDS).get();
        task.assertContextWasSet();
        task.assertContextWasReset();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForNormalExecution);
    }

    // trigger

    @Test
    public void testScheduleTriggerRunnable() throws Exception {
        TestRunnable task = new TestRunnable(3);
        TestTrigger trigger = new TestTrigger(task.toString(), false, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        future.get();
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerCallable() throws Exception {
        TestCallable task = new TestCallable(3);
        TestTrigger trigger = new TestTrigger(task.toString(), false, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        future.get();
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedRunnable() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(3, TestManagedTaskListener.expectedListenerCallbacksForTriggerExecution.length);
        TestTrigger trigger = new TestTrigger(task.toString(), false, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerExecution);
        future.get();
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedCallable() throws Exception {
        TestManagedCallable task = new TestManagedCallable(3, TestManagedTaskListener.expectedListenerCallbacksForTriggerExecution.length);
        task.identityName = UUID.randomUUID().toString();
        TestTrigger trigger = new TestTrigger(task.identityName, false, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerExecution);
        future.get();
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerRunnableSkipped() throws Exception {
        TestRunnable task = new TestRunnable(2);
        TestTrigger trigger = new TestTrigger(task.toString(), true, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        future.get();
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerCallableSkipped() throws Exception {
        TestCallable task = new TestCallable(2);
        TestTrigger trigger = new TestTrigger(task.toString(), true, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        future.get();
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedRunnableSkipped() throws Exception {
        TestManagedRunnable task = new TestManagedRunnable(2, TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipExecution.length);
        task.identityName = UUID.randomUUID().toString();
        TestTrigger trigger = new TestTrigger(task.identityName, true, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipExecution);
        future.get();
        assertFutureTermination(future, false);
    }

    @Test
    public void testScheduleTriggerManagedCallableSkipped() throws Exception {
        TestManagedCallable task = new TestManagedCallable(2, TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipExecution.length);
        TestTrigger trigger = new TestTrigger(task.toString(), true, false);
        ScheduledFuture future = scheduledExecutor.schedule(task, trigger);
        task.assertContextWasSet();
        task.assertContextWasReset();
        trigger.assertResults();
        TestManagedTaskListener listener = task.getTestManagedTaskListener();
        listener.assertListenerDone();
        listener.assertListenerGotExpectedCallbacks(TestManagedTaskListener.expectedListenerCallbacksForTriggerWithSkipExecution);
        future.get();
        assertFutureTermination(future, false);
    }

}
