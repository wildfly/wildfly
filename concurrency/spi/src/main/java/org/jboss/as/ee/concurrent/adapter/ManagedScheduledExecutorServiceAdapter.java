/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.adapter;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.Trigger;
import org.jboss.as.ee.concurrent.ManagedExecutorRuntimeStats;
import org.jboss.as.ee.concurrent.WildFlyManagedThreadFactory;
import org.jboss.as.ee.concurrent.WildFlyManagedScheduledExecutorService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Abstract base class for {@code ManagedExecutorService} and {@code ManagedScheduledExecutorService} implementation with life cycle operations disabled for handing out to application components.
 */
public class ManagedScheduledExecutorServiceAdapter extends AbstractManagedExecutorServiceAdapter implements WildFlyManagedScheduledExecutorService {

    private final WildFlyManagedScheduledExecutorService executorService;

    public ManagedScheduledExecutorServiceAdapter(WildFlyManagedScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public WildFlyManagedScheduledExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public WildFlyManagedThreadFactory getWildFlyManagedThreadFactory() {
        return executorService.getWildFlyManagedThreadFactory();
    }

    @Override
    public void terminateHungTasks() {
        executorService.terminateHungTasks();
    }

    @Override
    public ManagedExecutorRuntimeStats getRuntimeStats() {
        return executorService.getRuntimeStats();
    }

    @Override
    public <U> CompletableFuture<U> completedFuture(U u) {
        return executorService.completedFuture(u);
    }

    @Override
    public <U> CompletionStage<U> completedStage(U u) {
        return executorService.completedStage(u);
    }

    @Override
    public <T> CompletableFuture<T> copy(CompletableFuture<T> completableFuture) {
        return executorService.copy(completableFuture);
    }

    @Override
    public <T> CompletionStage<T> copy(CompletionStage<T> completionStage) {
        return executorService.copy(completionStage);
    }

    @Override
    public <U> CompletableFuture<U> failedFuture(Throwable throwable) {
        return executorService.failedFuture(throwable);
    }

    @Override
    public <U> CompletionStage<U> failedStage(Throwable throwable) {
        return executorService.failedStage(throwable);
    }

    @Override
    public ContextService getContextService() {
        return executorService.getContextService();
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return executorService.newIncompleteFuture();
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return executorService.runAsync(runnable);
    }

    @Override
    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return executorService.supplyAsync(supplier);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executorService.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(command);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, Trigger trigger) {
        return executorService.schedule(runnable, trigger);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, Trigger trigger) {
        return executorService.schedule(callable, trigger);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, java.util.concurrent.TimeUnit unit) {
        return executorService.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, java.util.concurrent.TimeUnit unit) {
        return executorService.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, java.util.concurrent.TimeUnit unit) {
        return executorService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, java.util.concurrent.TimeUnit unit) {
        return executorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
