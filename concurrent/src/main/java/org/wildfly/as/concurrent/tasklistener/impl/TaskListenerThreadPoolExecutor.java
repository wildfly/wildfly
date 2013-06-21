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
import org.wildfly.as.concurrent.tasklistener.TaskListenerExecutorService;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link java.util.concurrent.ThreadPoolExecutor} with {@link TaskListener} support, i.e. upon execution of tasks that implement {@link TaskListener}, the executor will be do the callbacks.
 *
 * @author Eduardo Martins
 */
public class TaskListenerThreadPoolExecutor extends ThreadPoolExecutor implements TaskListenerExecutorService {

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @see java.util.concurrent.ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, java.util.concurrent.TimeUnit, java.util.concurrent.BlockingQueue)
     */
    public TaskListenerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param threadFactory
     * @see java.util.concurrent.ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, java.util.concurrent.TimeUnit, java.util.concurrent.BlockingQueue, java.util.concurrent.ThreadFactory)
     */
    public TaskListenerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param handler
     * @see java.util.concurrent.ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, java.util.concurrent.TimeUnit, java.util.concurrent.BlockingQueue, java.util.concurrent.RejectedExecutionHandler)
     */
    public TaskListenerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param threadFactory
     * @param handler
     * @see java.util.concurrent.ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, java.util.concurrent.TimeUnit, java.util.concurrent.BlockingQueue, java.util.concurrent.ThreadFactory, java.util.concurrent.RejectedExecutionHandler)
     */
    public TaskListenerThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    // override submit and execute methods to abort task listeners if the operation does not succeeds

    @Override
    public Future<?> submit(Runnable task) {
        task = TaskListenerWrapperRunnable.wrap(task, false);
        Throwable t = null;
        try {
            return super.submit(task);
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
    public <T> Future<T> submit(Runnable task, T result) {
        task = TaskListenerWrapperRunnable.wrap(task, false);
        Throwable t = null;
        try {
            return super.submit(task, result);
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
    public <T> Future<T> submit(Callable<T> task) {
        task = TaskListenerWrapperCallable.wrap(task, false);
        Throwable t = null;
        try {
            return super.submit(task);
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
    public void execute(Runnable task) {
        if (!(task instanceof TaskListener)) {
            super.execute(task);
        } else {
            Throwable t = null;
            task = TaskListenerWrapperRunnable.wrap(task, false);
            try {
                super.execute(newTaskFor(task, null));
            } catch (RuntimeException e) {
                t = e;
                throw e;
            } catch (Error e) {
                t = e;
                throw e;
            } finally {
                if (t != null) {
                    ((TaskListenerWrapper) task).taskSubmitFailed(t);
                }
            }
        }
    }

    // override future maker methods, in case it's a task listener wrap both the task and future

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
        if (task instanceof TaskListenerWrapper) {
            return new RunnableFutureWrapper<>(super.newTaskFor(task), (TaskListenerWrapper) task);
        } else if (task instanceof TaskListener) {
            final TaskListenerWrapperCallable<T> taskWrapper = new TaskListenerWrapperCallable<>(task, false);
            return new RunnableFutureWrapper<>(super.newTaskFor(taskWrapper), taskWrapper);
        } else {
            return super.newTaskFor(task);
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable task, T value) {
        if (task instanceof TaskListenerWrapper) {
            return new RunnableFutureWrapper<>(super.newTaskFor(task, value), (TaskListenerWrapper) task);
        } else if (task instanceof TaskListener) {
            final TaskListenerWrapperRunnable taskWrapper = new TaskListenerWrapperRunnable(task, false);
            return new RunnableFutureWrapper<>(super.newTaskFor(taskWrapper, value), taskWrapper);
        } else {
            return super.newTaskFor(task, value);
        }
    }

}


