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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link java.util.concurrent.ThreadPoolExecutor} which is a {@link TaskDecoratorExecutorService}.
 *
 * @author Eduardo Martins
 */
public class TaskDecoratorThreadPoolExecutor extends ThreadPoolExecutor implements TaskDecoratorExecutorService {

    public TaskDecoratorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public TaskDecoratorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public TaskDecoratorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public TaskDecoratorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable task) {
        if (!(task instanceof TaskDecorator)) {
            super.execute(task);
        } else {
            super.execute(newTaskFor(task, null));
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
        final RunnableFuture<T> runnableFuture = super.newTaskFor(task);
        if (task instanceof TaskDecorator) {
            return ((TaskDecorator) task).decorateRunnableFuture(runnableFuture);
        } else {
            return runnableFuture;
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable task, T value) {
        if (task instanceof TaskDecorator) {
            return ((TaskDecorator) task).decorateRunnableFuture(super.newTaskFor(task, value));
        } else if (task instanceof RunnableFuture) {
            return (RunnableFuture<T>) task;
        } else {
            return super.newTaskFor(task, value);
        }
    }

}


