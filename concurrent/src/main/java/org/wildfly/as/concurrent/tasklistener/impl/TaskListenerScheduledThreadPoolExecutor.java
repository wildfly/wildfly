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

package org.wildfly.as.concurrent.tasklistener.impl;

import org.wildfly.as.concurrent.tasklistener.TaskListener;
import org.wildfly.as.concurrent.tasklistener.TaskListenerScheduledExecutorService;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link ScheduledThreadPoolExecutor} with {@link TaskListener} support, i.e. upon execution of tasks that implement {@link TaskListener}, the executor will be do the callbacks.
 *
 * @author Eduardo Martins
 */
public class TaskListenerScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor implements TaskListenerScheduledExecutorService {

    /**
     * @param corePoolSize
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int)
     */
    public TaskListenerScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    /**
     * @param corePoolSize
     * @param threadFactory
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, java.util.concurrent.ThreadFactory)
     */
    public TaskListenerScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    /**
     * @param corePoolSize
     * @param handler
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, java.util.concurrent.RejectedExecutionHandler)
     */
    public TaskListenerScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    /**
     * @param corePoolSize
     * @param threadFactory
     * @param handler
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, java.util.concurrent.ThreadFactory, java.util.concurrent.RejectedExecutionHandler)
     */
    public TaskListenerScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    // note: all submit and execute methods delegate to schedule, no need to override these

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        Throwable t = null;
        task = TaskListenerWrapperRunnable.wrap(task, false);
        try {
            return super.schedule(task, delay, unit);
        } catch (RuntimeException e) {
            t = e;
            throw e;
        } catch (Error e) {
            t = e;
            throw e;
        } finally {
            if (t != null && task instanceof TaskListenerWrapper) {
                ((TaskListenerWrapper) task).taskSubmitFailed(t);
            }
        }
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        Throwable t = null;
        task = TaskListenerWrapperCallable.wrap(task, false);
        try {
            return super.schedule(task, delay, unit);
        } catch (RuntimeException e) {
            t = e;
            throw e;
        } catch (Error e) {
            t = e;
            throw e;
        } finally {
            if (t != null && task instanceof TaskListenerWrapper) {
                ((TaskListenerWrapper) task).taskSubmitFailed(t);
            }
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        Throwable t = null;
        task = TaskListenerWrapperRunnable.wrap(task, true);
        try {
            return super.scheduleAtFixedRate(task, initialDelay, period, unit);
        } catch (RuntimeException e) {
            t = e;
            throw e;
        } catch (Error e) {
            t = e;
            throw e;
        } finally {
            if (t != null && task instanceof TaskListenerWrapper) {
                ((TaskListenerWrapper) task).taskSubmitFailed(t);
            }
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        Throwable t = null;
        task = TaskListenerWrapperRunnable.wrap(task, true);
        try {
            return super.scheduleWithFixedDelay(task, initialDelay, delay, unit);
        } catch (RuntimeException e) {
            t = e;
            throw e;
        } catch (Error e) {
            t = e;
            throw e;
        } finally {
            if (t != null && task instanceof TaskListenerWrapper) {
                ((TaskListenerWrapper) task).taskSubmitFailed(t);
            }
        }
    }

    // future makers

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        if (callable instanceof TaskListenerWrapper) {
            task = new RunnableScheduledFutureWrapper<>(task, (TaskListenerWrapper) callable);
        }
        return task;
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        if (runnable instanceof TaskListenerWrapper) {
            task = new RunnableScheduledFutureWrapper<>(task, (TaskListenerWrapper) runnable);
        } else if (runnable instanceof RunnableScheduledFutureWrapper) {
            // this may happen due to newTaskFor() usage by invokeAll()
            task = (RunnableScheduledFuture<V>) runnable;
        }
        return task;
    }

    // fixes for two issues with super impl

    // 1. fixing forgotten (by super) invokeAll()

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
        // invokeAll is not overwritten by ScheduledThreadPoolExecutor, i.e. uses AbstractExecutorService logic, and that relies on this method to create the futures returned
        boolean isTaskListener = task instanceof TaskListener;
        if (isTaskListener) {
            task = new TaskListenerWrapperCallable<>(task, false);
        }
        RunnableFuture<T> runnableFuture = super.newTaskFor(task);
        if (isTaskListener) {
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

    // 2. usage of Executors.callable(Runnable task, T result) on submit(Runnable task, T result), hides impl of TaskListener by the Runnable

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (task instanceof TaskListener) {
            return submit(new TaskListenerRunnableAdapter<>(task, result));
        } else {
            return super.submit(task, result);
        }
    }

    /**
     * A callable that runs given task and returns given result, but also implements TaskListener
     */
    static final class TaskListenerRunnableAdapter<T> implements Callable<T>, TaskListener {

        final Runnable task;
        final TaskListener taskListener;
        final T result;

        TaskListenerRunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.taskListener = (TaskListener) task;
            this.result = result;
        }

        @Override
        public T call() throws Exception {
            task.run();
            return result;
        }

        @Override
        public void taskSubmitted(Future<?> future) {
            taskListener.taskSubmitted(future);
        }

        @Override
        public void taskStarting() {
            taskListener.taskStarting();
        }

        @Override
        public void taskDone(Throwable exception) {
            taskListener.taskDone(exception);
        }
    }

}
