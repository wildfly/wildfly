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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Scheduler that uses a single scheduled task in concert with an {@link ScheduledEntries}.
 * @author Paul Ferraro
 */
public class LocalScheduler<T> implements Scheduler<T, Instant>, Iterable<T>, Runnable {

    private enum ThreadFactoryAction implements ParametricPrivilegedAction<ThreadFactory, Class<?>> {
        INSTANCE;

        @Override
        public ThreadFactory run(Class<?> targetClass) {
            ThreadFactory factory = new JBossThreadFactory(new ThreadGroup(targetClass.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
            return new ClassLoaderThreadFactory(factory, targetClass.getClassLoader());
        }
    }

    private enum ExecutorServiceAction implements ParametricPrivilegedAction<Void, ExecutorService> {
        SHUTDOWN;

        @Override
        public Void run(ExecutorService executor) {
            executor.shutdown();
            return null;
        }
    }

    private final ScheduledExecutorService executor;
    private final ScheduledEntries<T, Instant> entries;
    private final Predicate<T> task;

    private volatile Future<?> future = null;

    public LocalScheduler(ScheduledEntries<T, Instant> entries, Predicate<T> task) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, WildFlySecurityManager.doPrivilegedWithParameter(this.getClass(), ThreadFactoryAction.INSTANCE));
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(entries.isSorted());
        this.executor = executor;
        this.entries = entries;
        this.task = task;
    }

    @Override
    public void schedule(T id, Instant instant) {
        this.entries.add(id, instant);
        if (this.entries.isSorted()) {
            this.cancelIfPresent(id);
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
    public Iterator<T> iterator() {
        Iterator<Map.Entry<T, Instant>> entries = this.entries.iterator();
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return entries.hasNext();
            }

            @Override
            public T next() {
                return entries.next().getKey();
            }
        };
    }

    @Override
    public synchronized void close() {
        if (this.future != null) {
            if (!this.future.isDone()) {
                this.future.cancel(true);
            }
        }
        WildFlySecurityManager.doPrivilegedWithParameter(this.executor, ExecutorServiceAction.SHUTDOWN);
    }

    @Override
    public synchronized void run() {
        for (Map.Entry<T, Instant> entry : this.entries) {
            if (Thread.currentThread().isInterrupted()) return;
            if (entry.getValue().isAfter(Instant.now())) break;
            T key = entry.getKey();
            if (this.task.test(key)) {
                // If task execution fails, reschedule
                this.entries.remove(key);
            }
        }
        this.future = this.scheduleFirst();
    }

    private Future<?> scheduleFirst() {
        Map.Entry<T, Instant> entry = this.entries.peek();
        return (entry != null) ? this.schedule(entry) : null;
    }

    private Future<?> schedule(Map.Entry<T, Instant> entry) {
        Duration delay = Duration.between(Instant.now(), entry.getValue());
        long nanos = !delay.isNegative() ? delay.toNanos() : 0;
        try {
            return this.executor.schedule(this, nanos, TimeUnit.NANOSECONDS);
        } catch (RejectedExecutionException e) {
            return null;
        }
    }

    private void scheduleIfAbsent() {
        if (this.future == null) {
            // Exclusive with run()
            synchronized (this) {
                if (this.future == null) {
                    this.future = this.scheduleFirst();
                }
            }
        }
    }

    private void cancelIfPresent(T id) {
        if (this.future != null) {
            Map.Entry<T, Instant> first = this.entries.peek();
            if ((first != null) && first.getKey().equals(id)) {
                // Exclusive with run()
                synchronized (this) {
                    if (this.future != null) {
                        Map.Entry<T, Instant> entry = this.entries.peek();
                        if ((entry != null) && entry.getKey().equals(id)) {
                            this.future.cancel(true);
                            this.future = null;
                        }
                    }
                }
            }
        }
    }
}
