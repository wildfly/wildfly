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

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link java.util.concurrent.ScheduledThreadPoolExecutor} which is a {@link TaskDecoratorScheduledExecutorService}.
 *
 * @author Eduardo Martins
 */
public class TaskDecoratorScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor implements TaskDecoratorScheduledExecutorService {


    public TaskDecoratorScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public TaskDecoratorScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public TaskDecoratorScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public TaskDecoratorScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> task, RunnableScheduledFuture<V> runnableScheduledFuture) {
        if (task instanceof TaskDecorator) {
            return ((TaskDecorator) task).decorateRunnableScheduledFuture(runnableScheduledFuture);
        } else if (task instanceof RunnableAdapter) {
            return decorateTask(((RunnableAdapter) task).getRunnable(), runnableScheduledFuture);
        } else {
            return super.decorateTask(task, runnableScheduledFuture);
        }
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable task, RunnableScheduledFuture<V> runnableScheduledFuture) {
        if (task instanceof TaskDecorator) {
            return ((TaskDecorator) task).decorateRunnableScheduledFuture(runnableScheduledFuture);
        } else if (task instanceof RunnableScheduledFuture) {
            // this may happen due to newTaskFor() usage by invokeAll()
            return (RunnableScheduledFuture<V>) task;
        } else {
            return super.decorateTask(task, runnableScheduledFuture);
        }
    }

    // fixes for two issues with super impl

    // 1. fixing forgotten (by super) invokeAll()

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
        // invokeAll is not overwritten by ScheduledThreadPoolExecutor, i.e. uses AbstractExecutorService logic, and that relies on this method to create the futures returned
        RunnableFuture<T> runnableFuture = super.newTaskFor(task);
        if (task instanceof TaskDecorator) {
            runnableFuture = decorateTask(task, new RunnableFutureAdapter<>(runnableFuture));
        }
        return runnableFuture;
    }

    /**
     * A class which adapts a RunnableFuture to RunnableScheduledFuture.
     *
     * @author Eduardo Martins
     */
    private static final class RunnableFutureAdapter<V> implements RunnableScheduledFuture<V> {

        final RunnableFuture<V> runnableFuture;

        RunnableFutureAdapter(RunnableFuture<V> runnableFuture) {
            this.runnableFuture = runnableFuture;
        }

        @Override
        public void run() {
            runnableFuture.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return runnableFuture.cancel(mayInterruptIfRunning);
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

        @Override
        public long getDelay(TimeUnit unit) {
            return 0L;
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this)
                return 0;
            long d = -other.getDelay(TimeUnit.NANOSECONDS);
            return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
        }

        @Override
        public boolean isPeriodic() {
            return false;
        }
    }

    // 2. usage of Executors.callable(Runnable task, T result) on submit(Runnable task, T result), hides impl of TaskDecorator by the Runnable

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (task instanceof TaskDecorator) {
            return submit(new RunnableAdapter<>(task, result));
        } else {
            return super.submit(task, result);
        }
    }

    /**
     * A callable that runs given task and returns given result, but also exposes the original runnable.
     */
    static final class RunnableAdapter<T> implements Callable<T> {

        final Runnable task;

        final T result;

        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }

        @Override
        public T call() throws Exception {
            task.run();
            return result;
        }

        Runnable getRunnable() {
            return task;
        }

    }

}
