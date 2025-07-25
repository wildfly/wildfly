/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.Trigger;
import org.glassfish.concurro.AbstractManagedThread;
import org.glassfish.concurro.ContextServiceImpl;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.extension.requestcontroller.ControlPoint;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * WildFly's extension of {@link org.glassfish.concurro.ManagedScheduledExecutorServiceImpl}.
 *
 * @author Eduardo Martins
 */
public class ConcurroManagedScheduledExecutorServiceImpl extends org.glassfish.concurro.ManagedScheduledExecutorServiceImpl implements WildFlyManagedScheduledExecutorService {

    private final ControlPoint controlPoint;
    private final ProcessStateNotifier processStateNotifier;
    private final ManagedExecutorRuntimeStats runtimeStats;

    public ConcurroManagedScheduledExecutorServiceImpl(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        super(name, (ConcurroManagedThreadFactoryImpl) managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, (ContextServiceImpl) contextService, ConcurroManagedExecutorServiceImpl.convertRejectPolicy(rejectPolicy));
        this.controlPoint = controlPoint;
        this.processStateNotifier = processStateNotifier;
        this.runtimeStats = new ConcurroManagedExecutorRuntimeStatsImpl(this);
    }

    @Override
    public WildFlyManagedThreadFactory getWildFlyManagedThreadFactory() {
        return (WildFlyManagedThreadFactory) getManagedThreadFactory();
    }

    @Override
    public void execute(Runnable command) {
        super.execute(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doWrap(command, controlPoint, processStateNotifier)));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doWrap(task, controlPoint, processStateNotifier)));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doWrap(task, controlPoint, processStateNotifier)), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doWrap(task, controlPoint, processStateNotifier)));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, Trigger trigger) {
        final CancellableTrigger ctrigger = new CancellableTrigger(trigger);
        ctrigger.future = super.schedule(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doScheduledWrap(command, controlPoint, processStateNotifier)), ctrigger);
        return ctrigger.future;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, Trigger trigger) {
        final CancellableTrigger ctrigger = new CancellableTrigger(trigger);
        ctrigger.future = super.schedule(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doScheduledWrap(callable, controlPoint, processStateNotifier)), ctrigger);
        return ctrigger.future;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doScheduledWrap(command, controlPoint, processStateNotifier)), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doScheduledWrap(callable, controlPoint, processStateNotifier)), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doScheduledWrap(command, controlPoint, processStateNotifier)), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(SecurityIdentityUtils.doIdentityWrap(ControlPointUtils.doScheduledWrap(command, controlPoint, processStateNotifier)), initialDelay, delay, unit);
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

    @Override
    public void terminateHungTasks() {
        final String executorName = getClass().getSimpleName() + ":" + getName();
        EeLogger.ROOT_LOGGER.debugf("Cancelling %s hung tasks...", executorName);
        final Collection<Thread> hungThreads = getHungThreads();
        if (hungThreads != null) {
            for (Thread t : hungThreads) {
                final String taskIdentityName = ((AbstractManagedThread)t).getTaskIdentityName();
                try {
                    if (t instanceof ConcurroManagedThreadFactoryImpl.ManagedThread) {
                        if (((ConcurroManagedThreadFactoryImpl.ManagedThread)t).cancelTask()) {
                            EeLogger.ROOT_LOGGER.hungTaskCancelled(executorName, taskIdentityName);
                        } else {
                            EeLogger.ROOT_LOGGER.hungTaskNotCancelled(executorName, taskIdentityName);
                        }
                    }
                } catch (Throwable throwable) {
                    EeLogger.ROOT_LOGGER.huntTaskTerminationFailure(throwable, executorName, taskIdentityName);
                }
            }
        }
    }
}
