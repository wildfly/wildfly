/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service.concurrent;

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

    private final ExecutorService service;
    private final Contextualizer contextualizer;

    public ContextualExecutorService(ExecutorService service, Contextualizer contextualizer) {
        this.service = service;
        this.contextualizer = contextualizer;
    }

    @Override
    public void execute(Runnable command) {
        this.service.execute(this.contextualizer.contextualize(command));
    }

    @Override
    public void shutdown() {
        this.service.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.service.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.service.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.service.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.service.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.service.submit(this.contextualizer.contextualize(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.service.submit(this.contextualizer.contextualize(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.service.submit(this.contextualizer.contextualize(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.service.invokeAll(this.contextualize(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return this.service.invokeAll(this.contextualize(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return this.service.invokeAny(this.contextualize(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.service.invokeAny(this.contextualize(tasks), timeout, unit);
    }

    private <T> Collection<Callable<T>> contextualize(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> result = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            result.add(this.contextualizer.contextualize(task));
        }
        return result;
    }
}
