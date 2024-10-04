/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.jboss.as.controller.ProcessStateNotifier;
import org.wildfly.extension.requestcontroller.ControlPoint;

import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.Trigger;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.ee.concurrent.ControlPointUtils.doScheduledWrap;
import static org.jboss.as.ee.concurrent.ControlPointUtils.doWrap;
import static org.jboss.as.ee.concurrent.SecurityIdentityUtils.doIdentityWrap;

/**
 * WildFly's extension of {@link org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceImpl}.
 *
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceImpl extends org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceImpl implements ManagedExecutorWithHungThreads {

    private final ControlPoint controlPoint;
    private final ProcessStateNotifier processStateNotifier;
    private final ManagedExecutorRuntimeStats runtimeStats;

    public ManagedScheduledExecutorServiceImpl(String name, ManagedThreadFactoryImpl managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, ContextServiceImpl contextService, RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy);
        this.controlPoint = controlPoint;
        this.processStateNotifier = processStateNotifier;
        this.runtimeStats = new ManagedExecutorRuntimeStatsImpl(this);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(doIdentityWrap(doWrap(command, controlPoint, processStateNotifier)));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(doIdentityWrap(doWrap(task, controlPoint, processStateNotifier)));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(doIdentityWrap(doWrap(task, controlPoint, processStateNotifier)), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(doIdentityWrap(doWrap(task, controlPoint, processStateNotifier)));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, Trigger trigger) {
        final CancellableTrigger ctrigger = new CancellableTrigger(trigger);
        ctrigger.future = super.schedule(doIdentityWrap(doScheduledWrap(command, controlPoint, processStateNotifier)), ctrigger);
        return ctrigger.future;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, Trigger trigger) {
        final CancellableTrigger ctrigger = new CancellableTrigger(trigger);
        ctrigger.future = super.schedule(doIdentityWrap(doScheduledWrap(callable, controlPoint, processStateNotifier)), ctrigger);
        return ctrigger.future;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(doIdentityWrap(doScheduledWrap(command, controlPoint, processStateNotifier)), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(doIdentityWrap(doScheduledWrap(callable, controlPoint, processStateNotifier)), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(doIdentityWrap(doScheduledWrap(command, controlPoint, processStateNotifier)), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(doIdentityWrap(doScheduledWrap(command, controlPoint, processStateNotifier)), initialDelay, delay, unit);
    }

    @Override
    protected ThreadPoolExecutor getThreadPoolExecutor() {
        return (ThreadPoolExecutor) super.getThreadPoolExecutor();
    }

    /**
     *
     * @return the executor's runtime stats
     */
    public ManagedExecutorRuntimeStats getRuntimeStats() {
        return runtimeStats;
    }

    /**
     * A {@link jakarta.enterprise.concurrent.Trigger} wrapper that stops scheduling if the related {@link java.util.concurrent.ScheduledFuture} is cancelled.
     */
    private static class CancellableTrigger implements Trigger {
        private final Trigger trigger;
        private ScheduledFuture future;

        CancellableTrigger(Trigger trigger) {
            this.trigger = trigger;
        }

        @Override
        public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
            Date nextRunTime = trigger.getNextRunTime(lastExecution, taskScheduledTime);
            final ScheduledFuture future = this.future;
            if (future != null && future.isCancelled()) {
                nextRunTime = null;
            }
            return nextRunTime;
        }

        @Override
        public boolean skipRun(LastExecution lastExecution, Date date) {
            return trigger.skipRun(lastExecution, date);
        }
    }
}
