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

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author Eduardo Martins
 */
public class TestContextConfiguration implements ContextConfiguration {


    @Override
    public <V> Callable<V> newContextualCallable(Callable<V> callable) {
        return new ContextualCallable<>(callable);
    }

    @Override
    public Runnable newContextualRunnable(Runnable runnable) {
        return new ContextualRunnable(runnable);
    }

    @Override
    public ManagedTaskListener newContextualManagedTaskListener(ManagedTaskListener listener) {
        return new ContextualManagedTaskListener(listener);
    }

    @Override
    public InvocationHandler newContextualInvocationHandler(Object instance) {
        return new ContextualInvocationHandler(instance);
    }

    @Override
    public Trigger newContextualTrigger(Trigger trigger) {
        return new ContextualTrigger(trigger);
    }

    @Override
    public Runnable newManageableThreadContextualRunnable(Runnable runnable) {
        // while this creates a new context per runnable, it's enough for the unit tests
        return new ContextualRunnable(runnable);
    }

    private static TestContext setContext(TestContext context) {
        return context != null ? context.set() : null;
    }

    static interface TestContext {
        TestContext set();
    }

    private static class ContextualRunnable implements Runnable {

        private final Runnable runnable;
        private final TestContextImpl context;

        private ContextualRunnable(Runnable runnable) {
            this.runnable = runnable;
            this.context = new TestContextImpl(runnable);
        }

        @Override
        public void run() {
            final TestContext previous = setContext(context);
            try {
                runnable.run();
            } finally {
                setContext(previous);
            }
        }
    }

    private static class ContextualCallable<V> implements Callable<V> {

        private final Callable<V> callable;
        private final TestContextImpl context;

        private ContextualCallable(Callable<V> callable) {
            this.callable = callable;
            this.context = new TestContextImpl(callable);
        }

        @Override
        public V call() throws Exception {
            final TestContext previous = setContext(context);
            try {
                return callable.call();
            } finally {
                setContext(previous);
            }
        }
    }

    private static class ContextualManagedTaskListener implements ManagedTaskListener {

        private final ManagedTaskListener managedTaskListener;
        private final TestContextImpl context;

        private ContextualManagedTaskListener(ManagedTaskListener managedTaskListener) {
            this.managedTaskListener = managedTaskListener;
            this.context = new TestContextImpl(managedTaskListener);
        }

        @Override
        public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable exception) {
            final TestContext previous = setContext(context);
            try {
                managedTaskListener.taskAborted(future, executor, task, exception);
            } finally {
                setContext(previous);
            }
        }

        @Override
        public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable exception) {
            final TestContext previous = setContext(context);
            try {
                managedTaskListener.taskDone(future, executor, task, exception);
            } finally {
                setContext(previous);
            }
        }

        @Override
        public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
            final TestContext previous = setContext(context);
            try {
                managedTaskListener.taskStarting(future, executor, task);
            } finally {
                setContext(previous);
            }
        }

        @Override
        public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
            final TestContext previous = setContext(context);
            try {
                managedTaskListener.taskSubmitted(future, executor, task);
            } finally {
                setContext(previous);
            }
        }
    }

    private static class ContextualInvocationHandler implements InvocationHandler {

        private final Object instance;
        private final TestContextImpl context;


        private ContextualInvocationHandler(Object instance) {
            this.instance = instance;
            this.context = new TestContextImpl(instance);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final TestContext previous = setContext(context);
            try {
                return method.invoke(instance, args);
            } finally {
                setContext(previous);
            }
        }
    }

    private static class ContextualTrigger implements Trigger {

        private final Trigger trigger;
        private final TestContextImpl context;

        private ContextualTrigger(Trigger trigger) {
            this.trigger = trigger;
            this.context = new TestContextImpl(trigger);
        }

        @Override
        public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
            final TestContext previous = setContext(context);
            try {
                return trigger.getNextRunTime(lastExecutionInfo, taskScheduledTime);
            } finally {
                setContext(previous);
            }
        }

        @Override
        public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
            final TestContext previous = setContext(context);
            try {
                return trigger.skipRun(lastExecutionInfo, scheduledRunTime);
            } finally {
                setContext(previous);
            }
        }
    }

}
