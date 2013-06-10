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
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.SkippedException;
import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The {@link TaskWrapper}s created by a {@link TriggerScheduledFuture}, and responsible for the original task scheduling and execution.
 *
 * @author Eduardo Martins
 */
public abstract class TriggerTaskWrapper<V> extends TaskWrapper {

    protected final TriggerScheduledFuture<V> parent;
    protected final ManagedScheduledExecutorService scheduler;

    private LastExecution lastExecution;
    private Date scheduledStart;
    private LastExecutionImpl currentExecution;

    /**
     * @param task
     * @param scheduler
     * @param parent
     */
    public TriggerTaskWrapper(Object task, ManagedScheduledExecutorServiceImpl scheduler, TriggerScheduledFuture<V> parent) {
        super(task, false, scheduler.getContextConfiguration(), scheduler);
        this.scheduler = scheduler;
        this.parent = parent;
        this.scheduledStart = new Date();
    }

    /**
     * @return
     */
    protected LastExecutionImpl getCurrentExecution() {
        return currentExecution;
    }

    @Override
    protected void taskSubmitted(Future<?> future) {
        final ScheduledFutureWrapper<V> futureWrapper = new ScheduledFutureWrapper<>((ScheduledFuture<V>) future);
        super.taskSubmitted(futureWrapper);
        parent.runTaskSubmitted(futureWrapper);
    }

    @Override
    protected void taskStarting() {
        currentExecution.setRunStart(new Date());
        currentExecution.setSkipped(parent.getTrigger().skipRun(lastExecution, scheduledStart));
        super.taskStarting();
    }

    @Override
    protected void taskDone(Throwable exception) {
        if (exception == null && currentExecution.isSkipped()) {
            exception = new SkippedException();
        }
        super.taskDone(exception);
        if (exception == null) {
            currentExecution.setRunEnd(new Date());
        }
        parent.runTaskDone(currentExecution);
    }

    /**
     * @param lastExecution
     * @param nextRuntime
     */
    protected void scheduleNextRun(LastExecution lastExecution, Date nextRuntime) {
        this.currentExecution = new LastExecutionImpl();
        this.currentExecution.setIdentityName(getIdentityName());
        this.lastExecution = lastExecution;
        this.scheduledStart = nextRuntime;
        this.currentExecution.setScheduledStart(scheduledStart);
        long delay = nextRuntime.getTime() - System.currentTimeMillis();
        if (delay < 0L) {
            delay = 0L;
        }
        resetCallbacks();
        scheduleTask(delay, TimeUnit.MILLISECONDS);
    }

    /**
     * @param delay
     * @param timeUnit
     */
    protected abstract void scheduleTask(long delay, TimeUnit timeUnit);

    /**
     * A wrapper for {@link ScheduledFuture}, which intercepts get() methods and throws the spec SkippedException when required.
     *
     * @param <V>
     */
    private class ScheduledFutureWrapper<V> implements ScheduledFuture<V> {

        private final ScheduledFuture<V> future;

        private ScheduledFutureWrapper(ScheduledFuture<V> future) {
            this.future = future;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return future.getDelay(unit);
        }

        @Override
        public int compareTo(Delayed o) {
            return future.compareTo(o);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            final V v = future.get();
            if (currentExecution.isSkipped()) {
                throw new SkippedException();
            }
            return v;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            final V v = future.get(timeout, unit);
            if (currentExecution.isSkipped()) {
                throw new SkippedException();
            }
            return v;
        }
    }

}
