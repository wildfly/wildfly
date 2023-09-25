/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link ExecutorService} decorator that contextualizes tasks to be executed.
 * @author Paul Ferraro
 */
public class ContextualExecutorService implements ExecutorService {

    private final ExecutorService executor;
    private final Contextualizer contextualizer;

    public ContextualExecutorService(ExecutorService executor, Contextualizer contextualizer) {
        this.executor = executor;
        this.contextualizer = contextualizer;
    }

    @Override
    public void execute(Runnable command) {
        this.executor.execute(this.contextualizer.contextualize(command));
    }

    @Override
    public void shutdown() {
        this.executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.executor.submit(this.contextualizer.contextualize(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.executor.submit(this.contextualizer.contextualize(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.executor.submit(this.contextualizer.contextualize(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.executor.invokeAll(this.contextualize(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return this.executor.invokeAll(this.contextualize(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return this.executor.invokeAny(this.contextualize(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.executor.invokeAny(this.contextualize(tasks), timeout, unit);
    }

    private <T> Collection<Callable<T>> contextualize(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> result = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            result.add(this.contextualizer.contextualize(task));
        }
        return result;
    }
}
