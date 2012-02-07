/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.threads.JBossExecutors;

/**
 * ExecutorService decorator whose shutdown methods are no-ops.
 * @author Paul Ferraro
 */
public class ManagedExecutorService implements ExecutorService {
    private final ExecutorService executor;

    public ManagedExecutorService(ExecutorService executor) {
        this.executor = executor;
    }

    public ManagedExecutorService(Executor executor) {
        this(JBossExecutors.protectedExecutorService(executor));
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
     */
    @Override
    public void execute(Runnable command) {
        this.executor.execute(command);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#shutdown()
     */
    @Override
    public void shutdown() {
        // Don't shutdown managed executor
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#shutdownNow()
     */
    @Override
    public List<Runnable> shutdownNow() {
        // Don't shutdown managed executor
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#isShutdown()
     */
    @Override
    public boolean isShutdown() {
        return this.executor.isShutdown();
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#isTerminated()
     */
    @Override
    public boolean isTerminated() {
        return this.executor.isTerminated();
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executor.isTerminated();
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#submit(java.util.concurrent.Callable)
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.executor.submit(task);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable, java.lang.Object)
     */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.executor.submit(task, result);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
     */
    @Override
    public Future<?> submit(Runnable task) {
        return this.executor.submit(task);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection)
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.executor.invokeAll(tasks);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return this.executor.invokeAll(tasks, timeout, unit);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection)
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return this.executor.invokeAny(tasks);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.executor.invokeAny(tasks, timeout, unit);
    }
}
