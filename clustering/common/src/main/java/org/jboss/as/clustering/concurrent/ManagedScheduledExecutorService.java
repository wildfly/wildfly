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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * ScheduledExecutorService decorator that records recurring tasks and cancels them on shutdown.
 * @author Paul Ferraro
 */
public class ManagedScheduledExecutorService extends ManagedExecutorService implements ScheduledExecutorService {
    private final List<Future<?>> futures = new LinkedList<Future<?>>();
    private volatile boolean shutdown = false;
    private volatile boolean terminated = false;
    private final ScheduledExecutorService executor;

    public ManagedScheduledExecutorService(ScheduledExecutorService executor) {
        super(executor);
        this.executor = executor;
    }

    @Override
    public void shutdown() {
        this.shutdown(false);
    }

    @Override
    public List<Runnable> shutdownNow() {
        this.shutdown(true);
        return Collections.emptyList();
    }

    private void shutdown(boolean interrupt) {
        synchronized (this.futures) {
            if (this.shutdown) return;
            this.shutdown = true;

            for (Future<?> future: this.futures) {
                if (!future.isDone()) {
                    future.cancel(interrupt);
                }
            }
            if (!interrupt) {
                for (Future<?> future: this.futures) {
                    if (!future.isDone()) {
                        try {
                            future.get();
                        } catch (ExecutionException e) {
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            this.futures.clear();
            this.terminated = true;
            this.futures.notify();
        }
    }

    @Override
    public boolean isShutdown() {
        return this.shutdown;
    }

    @Override
    public boolean isTerminated() {
        return this.terminated;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (this.terminated) return true;
        synchronized (this.futures) {
            this.futures.wait(unit.toMillis(timeout));
        }
        return this.terminated;
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return this.executor.schedule(command, delay, unit);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return this.executor.schedule(callable, delay, unit);
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        synchronized (this.futures) {
            if (this.shutdown) throw new RejectedExecutionException();
            ScheduledFuture<?> future = this.executor.scheduleAtFixedRate(command, initialDelay, period, unit);
            this.futures.add(future);
            return future;
        }
    }

    /**
     * {@inheritDoc}
     * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        synchronized (this.futures) {
            if (this.shutdown) throw new RejectedExecutionException();
            ScheduledFuture<?> future = this.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
            this.futures.add(future);
            return future;
        }
    }
}
