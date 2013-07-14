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

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.SkippedException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The base wrapper for Runnable/Callable tasks submitted to {@link ManagedExecutorServiceImpl}s.
 *
 * @author Eduardo Martins
 */
abstract class TaskWrapper implements TaskDecoratorScheduledExecutorService.TaskDecorator {

    private final Object task;
    private final ManagedTaskListener managedTaskListener;
    private final String identityName;
    private final ManagedExecutorServiceImpl executor;

    private final boolean periodic;
    private final CallbacksLock callbacksLock = new CallbacksLock();
    private Future<?> future;

    /**
     * @param task
     * @param periodic
     * @param contextConfiguration
     * @param executor
     */
    TaskWrapper(Object task, boolean periodic, ContextConfiguration contextConfiguration, ManagedExecutorServiceImpl executor) {
        this.task = task;
        if (task instanceof javax.enterprise.concurrent.ManagedTask) {
            final javax.enterprise.concurrent.ManagedTask managedTask = (javax.enterprise.concurrent.ManagedTask) task;
            ManagedTaskConfiguration managedTaskConfiguration = new ManagedTaskConfiguration(managedTask);
            final ManagedTaskListener managedTaskListener = managedTask.getManagedTaskListener();
            if (managedTaskListener == null || contextConfiguration == null) {
                this.managedTaskListener = managedTaskListener;
            } else {
                if (managedTaskConfiguration.isManagedTaskWithContextualCallbacks()) {
                    this.managedTaskListener = contextConfiguration.newContextualManagedTaskListener(managedTaskListener);
                } else {
                    this.managedTaskListener = managedTaskListener;
                }
            }
            this.identityName = managedTaskConfiguration.getIdentityName();
        } else {
            this.managedTaskListener = null;
            this.identityName = task.toString();
        }
        this.periodic = periodic;
        this.executor = executor;
    }

    /**
     * @return
     */
    String getIdentityName() {
        return identityName;
    }

    @Override
    public String toString() {
        // do not change this, specs define task.toString() as fallback to obtain task identity name, here we ensure the fallback is consistent with wrapped task, in case it's a spec ManagedTask
        return identityName;
    }

    protected void taskSubmitted(Future<?> future) {
        if (callbacksLock.lock()) {
            this.future = future;
            try {
                executor.taskSubmitted(future);
                if (managedTaskListener != null) {
                    try {
                        managedTaskListener.taskSubmitted(future, executor, task);
                    } catch (Throwable t) {
                        EeConcurrentLogger.ROOT_LOGGER.failureInTaskSubmittedCallback(t);
                    }
                }
            } finally {
                callbacksLock.unlock();
            }
            if (future.isCancelled()) {
                // a concurrent cancel may have not obtained the callbacks lock...
                taskCancelled();
            }
        }
    }

    protected void taskSubmitFailed(Throwable throwable) {
        if (callbacksLock.lock()) {
            if (future != null) {
                taskDone(throwable);
            }
            // do not unlock callbacks to avoid possible concurrent callbacks
        }
    }

    protected void beforeExecution() {
        if (callbacksLock.lock()) {
            try {
                taskStarting();
            } finally {
                callbacksLock.unlock();
            }
            if (future.isCancelled()) {
                // a concurrent cancel may have not obtained the callbacks lock...
                taskCancelled();
            }
        }
    }

    protected void taskStarting() {
        if (managedTaskListener != null) {
            try {
                managedTaskListener.taskStarting(future, executor, task);
            } catch (Throwable t) {
                EeConcurrentLogger.ROOT_LOGGER.failureInTaskStartingCallback(t);
            }
        }
    }

    protected void afterExecution(Throwable throwable) {
        if (callbacksLock.lock()) {
            // do not unlock callbacks unless task will be resubmitted, this avoids possible concurrent callbacks with cancellations
            if (future.isCancelled()) {
                taskDone(new CancellationException());
            } else {
                taskDone(throwable);
                if (throwable == null && periodic) {
                    resetCallbacks();
                    taskSubmitted(future);
                }
            }
        }
    }

    protected void taskDone(Throwable exception) {
        executor.taskDone(future);
        if (managedTaskListener != null) {
            try {
                if (exception != null) {
                    if (!(exception instanceof SkippedException) && !(exception instanceof CancellationException)) {
                        exception = new AbortedException(exception);
                    }
                    managedTaskListener.taskAborted(future, executor, task, exception);
                }
                managedTaskListener.taskDone(future, executor, task, exception);
            } catch (Throwable t) {
                EeConcurrentLogger.ROOT_LOGGER.failureInTaskDoneCallback(t);
            }
        }
    }

    protected void taskCancelled() {
        if (callbacksLock.lock()) {
            if (future != null) {
                taskDone(new CancellationException());
            }
            // do not unlock callbacks to avoid possible concurrent callbacks
        }
    }

    protected void resetCallbacks() {
        callbacksLock.unlock();
    }

    /**
     * A lock to avoid issues with concurrent callbacks
     */
    static class CallbacksLock {

        private final AtomicBoolean lock = new AtomicBoolean(false);

        boolean lock() {
            return lock.compareAndSet(false, true);
        }

        void unlock() {
            lock.set(false);
        }
    }

    // task decorator impl

    @Override
    public <T> RunnableScheduledFuture<T> decorateRunnableScheduledFuture(RunnableScheduledFuture<T> runnableScheduledFuture) {
        return new RunnableScheduledFutureWrapper<>(runnableScheduledFuture);
    }

    @Override
    public <T> RunnableFuture<T> decorateRunnableFuture(RunnableFuture<T> runnableFuture) {
        return new RunnableFutureWrapper<>(runnableFuture);
    }

    /**
     * Wrapper for {@link RunnableFuture}, responsible for doing the {@link TaskWrapper#taskSubmitted(java.util.concurrent.Future)} callback on instance creation, and {@link TaskWrapper#taskCancelled()} callbacks on successful cancels.
     *
     * @author Eduardo Martins
     */
    class RunnableFutureWrapper<V> implements RunnableFuture<V> {

        private final RunnableFuture<V> runnableFuture;

        RunnableFutureWrapper(RunnableFuture<V> runnableFuture) {
            this.runnableFuture = runnableFuture;
            taskSubmitted(this);
        }

        @Override
        public void run() {
            runnableFuture.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (runnableFuture.cancel(mayInterruptIfRunning)) {
                taskCancelled();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean isCancelled() {
            return runnableFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return runnableFuture.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return runnableFuture.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return runnableFuture.get(timeout, unit);
        }

    }

    /**
     * Wrapper for {@link java.util.concurrent.RunnableScheduledFuture}, responsible for doing the {@link TaskWrapper#taskSubmitted(java.util.concurrent.Future)} callback on instance creation, and {@link TaskWrapper#taskCancelled()} callbacks on successful cancels.
     *
     * @author Eduardo Martins
     */
    class RunnableScheduledFutureWrapper<V> implements RunnableScheduledFuture<V> {

        final RunnableScheduledFuture<V> runnableScheduledFuture;

        RunnableScheduledFutureWrapper(RunnableScheduledFuture<V> runnableScheduledFuture) {
            this.runnableScheduledFuture = runnableScheduledFuture;
            taskSubmitted(this);
        }

        @Override
        public boolean isPeriodic() {
            return runnableScheduledFuture.isPeriodic();
        }

        @Override
        public void run() {
            runnableScheduledFuture.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (runnableScheduledFuture.cancel(mayInterruptIfRunning)) {
                taskCancelled();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean isCancelled() {
            return runnableScheduledFuture.isCancelled();
        }

        @Override
        public boolean isDone() {
            return runnableScheduledFuture.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return runnableScheduledFuture.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return runnableScheduledFuture.get(timeout, unit);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return runnableScheduledFuture.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return runnableScheduledFuture.compareTo(o);
        }
    }
}
