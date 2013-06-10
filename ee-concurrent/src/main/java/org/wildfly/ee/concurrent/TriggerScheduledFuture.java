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

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The future which controls a trigger task execution.
 *
 * @author Eduardo Martins
 */
class TriggerScheduledFuture<V> implements ScheduledFuture<V> {

    private final TriggerTaskWrapper<V> task;
    private final Trigger trigger;
    private final ManagedScheduledExecutorServiceImpl executorService;

    private final Date scheduledStart = new Date();
    private ScheduledFuture<V> currentFuture;

    private final AtomicBoolean done = new AtomicBoolean(false);

    /**
     * @param runnable
     * @param trigger
     * @param executorService
     */
    TriggerScheduledFuture(Runnable runnable, Trigger trigger, ManagedScheduledExecutorServiceImpl executorService) {
        final TriggerTaskWrapperRunnable<V> rTask = new TriggerTaskWrapperRunnable<V>(runnable, executorService, this);
        this.task = rTask;
        final ContextConfiguration contextConfiguration = executorService.getContextConfiguration();
        this.trigger = contextConfiguration != null ? contextConfiguration.newContextualTrigger(trigger) : trigger;
        this.executorService = executorService;
        scheduleNextRun(null);
    }

    /**
     * @param callable
     * @param trigger
     * @param executorService
     */
    TriggerScheduledFuture(Callable<V> callable, Trigger trigger, ManagedScheduledExecutorServiceImpl executorService) {
        final TriggerTaskWrapperCallable<V> cTask = new TriggerTaskWrapperCallable<>(callable, executorService, this);
        this.task = cTask;
        final ContextConfiguration contextConfiguration = executorService.getContextConfiguration();
        this.trigger = contextConfiguration != null ? contextConfiguration.newContextualTrigger(trigger) : trigger;
        this.executorService = executorService;
        scheduleNextRun(null);
    }

    /**
     * @return
     */
    Trigger getTrigger() {
        return trigger;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return currentFuture == null ? 0L : currentFuture.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this) {
            return 0;
        }
        final long delayDiff = this.getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
        if (delayDiff == 0) {
            return 0;
        }
        return delayDiff < 0 ? -1 : 1;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done.compareAndSet(false, true)) {
            return currentFuture.cancel(mayInterruptIfRunning);
        } else {
            // task already done
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        return currentFuture == null ? false : currentFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return currentFuture == null ? null : currentFuture.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return currentFuture == null ? null : currentFuture.get(timeout, unit);
    }

    /**
     * @param lastExecution
     */
    void scheduleNextRun(LastExecution lastExecution) {
        final Date nextRuntime = trigger.getNextRunTime(lastExecution, scheduledStart);
        if (nextRuntime == null) {
            // task done
            if (done.compareAndSet(false, true)) {
                if (currentFuture == null) {
                    EeConcurrentLogger.ROOT_LOGGER.triggerTaskNeverSubmittedDueToNullGetNextRunTime();
                }
            }
        } else {
            if (!isDone()) {
                try {
                    task.scheduleNextRun(lastExecution, nextRuntime);
                } catch (Throwable e) {
                    EeConcurrentLogger.ROOT_LOGGER.failureWhenSchedulingNextRun(e);
                    if (done.compareAndSet(false, true)) {
                        if (currentFuture == null) {
                            EeConcurrentLogger.ROOT_LOGGER.triggerTaskNeverSubmittedDueToFailureWhenSchedulingNextRun();
                        }
                    }
                }
            }
        }
    }

    /**
     * @param future
     */
    void runTaskSubmitted(ScheduledFuture<V> future) {
        currentFuture = future;
    }

    /**
     * @param lastExecution
     */
    void runTaskDone(LastExecution lastExecution) {
        scheduleNextRun(lastExecution);
    }

}
