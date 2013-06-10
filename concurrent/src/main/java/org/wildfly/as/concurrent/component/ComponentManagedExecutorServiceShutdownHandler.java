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

package org.wildfly.as.concurrent.component;

import org.wildfly.as.concurrent.ConcurrentMessages;
import org.wildfly.as.concurrent.tasklistener.TaskListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handler for all lifecycle related {@link ComponentManagedExecutorService} methods, does book keeping of tasks being executed throw wrapping original tasks.
 *
 * @author Eduardo Martins
 */
public class ComponentManagedExecutorServiceShutdownHandler {

    private final Set<Future> futures;
    private volatile boolean internalShutdown;
    private final ReentrantLock lock;

    /**
     *
     */
    public ComponentManagedExecutorServiceShutdownHandler() {
        this.futures = new HashSet<>();
        this.lock = new ReentrantLock();
    }

    /**
     * @see org.wildfly.as.concurrent.component.ComponentManagedExecutorService#internalShutdown()
     */
    public void internalShutdown() {
        lock.lock();
        try {
            if (internalShutdown) {
                return;
            }
            internalShutdown = true;
            for (Future future : futures) {
                try {
                    future.cancel(true);
                } catch (Throwable throwable) {
                    // ignore
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @see org.wildfly.as.concurrent.component.ComponentManagedExecutorService#shutdown()
     */
    public void shutdown() {
        throw ConcurrentMessages.MESSAGES.lifecycleOfManagedExecutorServiceIsDoneByTheApplicationServer();
    }

    /**
     * @return
     * @see org.wildfly.as.concurrent.component.ComponentManagedExecutorService#shutdownNow()
     */
    public List<Runnable> shutdownNow() {
        throw ConcurrentMessages.MESSAGES.lifecycleOfManagedExecutorServiceIsDoneByTheApplicationServer();
    }

    /**
     * @return
     * @see org.wildfly.as.concurrent.component.ComponentManagedExecutorService#isShutdown()
     */
    public boolean isShutdown() {
        return false;
    }

    /**
     * @return
     * @see org.wildfly.as.concurrent.component.ComponentManagedExecutorService#isTerminated()
     */
    public boolean isTerminated() {
        return false;
    }

    /**
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException * @see org.wildfly.as.concurrent.component.ComponentManagedExecutorService#awaitTermination()
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw ConcurrentMessages.MESSAGES.lifecycleOfManagedExecutorServiceIsDoneByTheApplicationServer();
    }

    /**
     * Wraps the task as a {@link ShutdownHandlerTaskListener}.
     *
     * @param task
     * @return
     */
    public Runnable wrap(Runnable task) {
        if (internalShutdown) {
            throw ConcurrentMessages.MESSAGES.managedExecutorServiceShutdown();
        }
        return new ShutdownHandlerTaskListenerRunnable(task);
    }

    /**
     * Wraps the task as a {@link ShutdownHandlerTaskListener}.
     *
     * @param task
     * @param <V>
     * @return
     */
    public <V> Callable<V> wrap(Callable<V> task) {
        if (internalShutdown) {
            throw ConcurrentMessages.MESSAGES.managedExecutorServiceShutdown();
        }
        // wrap to get notified about task status
        return new ShutdownHandlerTaskListenerCallable<>(task);
    }

    /**
     * Task wrapping as a {@link TaskListener}, so this executor gets notified when task is submit and done, and know which tasks to cancel on shutdown
     */
    abstract class ShutdownHandlerTaskListener implements TaskListener {

        private final TaskListener taskListener;
        private Future<?> future;

        /**
         * @param task
         */
        ShutdownHandlerTaskListener(Object task) {
            this.taskListener = task instanceof TaskListener ? (TaskListener) task : null;
        }

        @Override
        public void taskSubmitted(Future<?> future) {
            lock.lock();
            try {
                this.future = future;
                try {
                    if (taskListener != null) {
                        taskListener.taskSubmitted(future);
                    }
                } finally {
                    if (internalShutdown) {
                        try {
                            future.cancel(true);
                        } catch (Throwable throwable) {
                            // ignore
                        }
                    } else {
                        futures.add(future);
                    }
                }
            } finally {
                lock.unlock();
            }

        }

        @Override
        public void taskStarting() {
            if (taskListener != null) {
                taskListener.taskStarting();
            }
        }

        @Override
        public void taskDone(Throwable exception) {
            lock.lock();
            try {
                try {
                    if (taskListener != null) {
                        taskListener.taskDone(exception);
                    }
                } finally {
                    if (!internalShutdown) {
                        futures.remove(future);
                    }
                }
            } finally {
                lock.unlock();
            }

        }
    }

    /**
     * A {@link Runnable} {@link ShutdownHandlerTaskListener}.
     */
    class ShutdownHandlerTaskListenerRunnable extends ShutdownHandlerTaskListener implements Runnable {

        private final Runnable task;

        ShutdownHandlerTaskListenerRunnable(Runnable task) {
            super(task);
            this.task = task;
        }

        @Override
        public void run() {
            task.run();
        }

        @Override
        public String toString() {
            // propagates identity name
            return task.toString();
        }
    }

    /**
     * A {@link Callable} {@link ShutdownHandlerTaskListener}.
     *
     * @param <V>
     */
    class ShutdownHandlerTaskListenerCallable<V> extends ShutdownHandlerTaskListener implements Callable<V> {

        private final Callable<V> task;

        ShutdownHandlerTaskListenerCallable(Callable<V> task) {
            super(task);
            this.task = task;
        }

        @Override
        public V call() throws Exception {
            return task.call();
        }

        @Override
        public String toString() {
            // propagates identity name
            return task.toString();
        }
    }
}
