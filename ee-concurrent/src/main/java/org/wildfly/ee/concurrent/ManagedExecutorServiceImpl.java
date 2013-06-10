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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link javax.enterprise.concurrent.ManagedExecutorService} which wraps tasks so these get invoked with a specific context set, and delegates executions into a {@link TaskDecoratorExecutorService}.
 *
 * @author Eduardo Martins
 */
public class ManagedExecutorServiceImpl implements ComponentManagedExecutorService {

    private final TaskDecoratorExecutorService taskDecoratorExecutorService;
    private final ContextConfiguration contextConfiguration;

    private final Map<Future, Object> futures;
    private static final Object FUTURES_VALUE = new Object();
    private volatile boolean internalShutdown;

    public ManagedExecutorServiceImpl(TaskDecoratorExecutorService taskDecoratorExecutorService, ContextConfiguration contextConfiguration) {
        this.taskDecoratorExecutorService = taskDecoratorExecutorService;
        this.contextConfiguration = contextConfiguration;
        this.futures = new ConcurrentHashMap<>();
    }

    ContextConfiguration getContextConfiguration() {
        return contextConfiguration;
    }

    TaskDecoratorExecutorService getTaskDecoratorExecutorService() {
        return taskDecoratorExecutorService;
    }

    // shutdown handling & future book keeping

    public synchronized void internalShutdown() {
        if (internalShutdown) {
            return;
        }
        internalShutdown = true;
        while (!futures.isEmpty()) {
            Iterator<Future> iterator = futures.keySet().iterator();
            while (iterator.hasNext()) {
                Future future = iterator.next();
                iterator.remove();
                try {
                    future.cancel(true);
                } catch (Throwable throwable) {
                    // ignore
                }
            }
        }
    }

    protected void taskSubmitted(Future<?> future) {
        if (internalShutdown) {
            try {
                future.cancel(true);
            } catch (Throwable throwable) {
                // ignore
            }
        } else {
            futures.put(future, FUTURES_VALUE);
            // double check since internal shutdown in theory may between the if condition and future put
            if (internalShutdown) {
                try {
                    future.cancel(true);
                } catch (Throwable throwable) {
                    // ignore
                }
                futures.remove(future);
            }
        }
    }

    protected void taskDone(Future<?> future) {
        if (!internalShutdown) {
            futures.remove(future);
        }
    }

    protected void checkShutdownState() throws RejectedExecutionException {
        if (internalShutdown) {
            throw EeConcurrentMessages.MESSAGES.managedExecutorServiceShutdown();
        }
    }

    // forbidding lifecycle changes through ExecutorService

    @Override
    public void shutdown() {
        // all lifecycle related methods are forbidden by spec
        throw EeConcurrentMessages.MESSAGES.lifecycleOfManagedExecutorServiceIsDoneByTheApplicationServer();
    }

    @Override
    public List<Runnable> shutdownNow() {
        // all lifecycle related methods are forbidden by spec
        throw EeConcurrentMessages.MESSAGES.lifecycleOfManagedExecutorServiceIsDoneByTheApplicationServer();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    // ExecutorService delegation

    @Override
    public Future<?> submit(Runnable task) {
        task = wrap(task, false);
        try {
            return taskDecoratorExecutorService.submit(task);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        task = wrap(task, false);
        try {
            return taskDecoratorExecutorService.submit(task, result);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        task = wrap(task, false);
        try {
            return taskDecoratorExecutorService.submit(task);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    @Override
    public void execute(Runnable task) {
        task = wrap(task, false);
        try {
            taskDecoratorExecutorService.execute(task);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        tasks = wrap(tasks);
        try {
            return taskDecoratorExecutorService.invokeAll(tasks);
        } catch (RuntimeException | Error | InterruptedException e) {
            for (Callable<T> task : tasks) {
                if (task instanceof TaskWrapper) {
                    ((TaskWrapper) task).taskSubmitFailed(e);
                }
            }
            throw e;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        tasks = wrap(tasks);
        try {
            return taskDecoratorExecutorService.invokeAll(tasks, timeout, unit);
        } catch (RuntimeException | Error | InterruptedException e) {
            for (Callable<T> task : tasks) {
                if (task instanceof TaskWrapper) {
                    ((TaskWrapper) task).taskSubmitFailed(e);
                }
            }
            throw e;
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        tasks = wrap(tasks);
        try {
            return taskDecoratorExecutorService.invokeAny(tasks);
        } catch (RuntimeException | Error | InterruptedException | ExecutionException e) {
            for (Callable<T> task : tasks) {
                if (task instanceof TaskWrapper) {
                    ((TaskWrapper) task).taskSubmitFailed(e);
                }
            }
            throw e;
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        tasks = wrap(tasks);
        try {
            return taskDecoratorExecutorService.invokeAny(tasks, timeout, unit);
        } catch (RuntimeException | Error | InterruptedException | ExecutionException e) {
            for (Callable<T> task : tasks) {
                if (task instanceof TaskWrapper) {
                    ((TaskWrapper) task).taskSubmitFailed(e);
                }
            }
            throw e;
        }
    }

    // task wrapping

    protected Runnable wrap(Runnable task, boolean periodic) throws RejectedExecutionException {
        checkShutdownState();
        if (task instanceof TaskWrapper) {
            return task;
        }
        return new TaskWrapperRunnable(task, periodic, contextConfiguration, this);
    }

    protected <V> Callable<V> wrap(Callable<V> task, boolean periodic) throws RejectedExecutionException {
        checkShutdownState();
        if (task instanceof TaskWrapper) {
            return task;
        }
        return new TaskWrapperCallable<>(task, periodic, contextConfiguration, this);
    }

    protected <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        final List<Callable<T>> wrappedTasks = new ArrayList<>();
        for (Callable<T> callable : tasks) {
            wrappedTasks.add(wrap(callable, false));
        }
        return wrappedTasks;
    }

}
