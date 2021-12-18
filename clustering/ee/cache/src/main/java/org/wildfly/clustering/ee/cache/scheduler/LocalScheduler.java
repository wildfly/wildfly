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

package org.wildfly.clustering.ee.cache.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.wildfly.clustering.context.DefaultExecutorService;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Scheduler that uses a single scheduled task in concert with an {@link ScheduledEntries}.
 * @author Paul Ferraro
 */
public class LocalScheduler<T> implements Scheduler<T, Instant>, Runnable {

    private final ScheduledExecutorService executor;
    private final ScheduledEntries<T, Instant> entries;
    private final Predicate<T> task;
    private final Duration closeTimeout;

    private volatile Map.Entry<Map.Entry<T, Instant>, Future<?>> futureEntry = null;

    public LocalScheduler(ScheduledEntries<T, Instant> entries, Predicate<T> task, Duration closeTimeout) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory(this.getClass()));
        executor.setKeepAliveTime(1L, TimeUnit.MINUTES);
        executor.allowCoreThreadTimeOut(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(entries.isSorted());
        this.executor = executor;
        this.entries = entries;
        this.task = task;
        this.closeTimeout = closeTimeout;
    }

    @Override
    public void schedule(T id, Instant instant) {
        this.entries.add(id, instant);
        if (this.entries.isSorted()) {
            this.rescheduleIfEarlier(instant);
        }
        this.scheduleIfAbsent();
    }

    @Override
    public void cancel(T id) {
        if (this.entries.isSorted()) {
            this.cancelIfPresent(id);
        }
        this.entries.remove(id);
        if (this.entries.isSorted()) {
            this.scheduleIfAbsent();
        }
    }

    @Override
    public boolean contains(T id) {
        return this.entries.contains(id);
    }

    @Override
    public Stream<T> stream() {
        return this.entries.stream().map(Map.Entry::getKey);
    }

    @Override
    public void close() {
        WildFlySecurityManager.doPrivilegedWithParameter(this.executor, DefaultExecutorService.SHUTDOWN_ACTION);
        if (!this.closeTimeout.isNegative() && !this.closeTimeout.isZero()) {
            try {
                this.executor.awaitTermination(this.closeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        Iterator<Map.Entry<T, Instant>> entries = this.entries.iterator();
        while (entries.hasNext()) {
            if (Thread.currentThread().isInterrupted() || this.executor.isShutdown()) return;
            Map.Entry<T, Instant> entry = entries.next();
            if (entry.getValue().isAfter(Instant.now())) break;
            T key = entry.getKey();
            // Remove only if task is successful
            if (this.task.test(key)) {
                entries.remove();
            }
        }
        synchronized (this) {
            this.futureEntry = this.scheduleFirst();
        }
    }

    private Map.Entry<Map.Entry<T, Instant>, Future<?>> scheduleFirst() {
        Map.Entry<T, Instant> entry = this.entries.peek();
        return (entry != null) ? this.schedule(entry) : null;
    }

    private Map.Entry<Map.Entry<T, Instant>, Future<?>> schedule(Map.Entry<T, Instant> entry) {
        Duration delay = Duration.between(Instant.now(), entry.getValue());
        long millis = !delay.isNegative() ? delay.toMillis() + 1 : 0;
        try {
            Future<?> future = this.executor.schedule(this, millis, TimeUnit.MILLISECONDS);
            return new SimpleImmutableEntry<>(entry, future);
        } catch (RejectedExecutionException e) {
            return null;
        }
    }

    private void scheduleIfAbsent() {
        if (this.futureEntry == null) {
            synchronized (this) {
                if (this.futureEntry == null) {
                    this.futureEntry = this.scheduleFirst();
                }
            }
        }
    }

    private void rescheduleIfEarlier(Instant instant) {
        if (this.futureEntry != null) {
            synchronized (this) {
                if (this.futureEntry != null) {
                    if (instant.isBefore(this.futureEntry.getKey().getValue())) {
                        this.futureEntry.getValue().cancel(true);
                        this.futureEntry = this.scheduleFirst();
                    }
                }
            }
        }
    }

    private void cancelIfPresent(T id) {
        if (this.futureEntry != null) {
            synchronized (this) {
                if (this.futureEntry != null) {
                    if (this.futureEntry.getKey().getKey().equals(id)) {
                        this.futureEntry.getValue().cancel(true);
                        this.futureEntry = null;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return this.entries.toString();
    }
}
