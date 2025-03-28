/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import org.glassfish.concurro.AbstractManagedExecutorService;
import org.glassfish.concurro.AbstractManagedThread;
import org.glassfish.concurro.ContextServiceImpl;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.extension.requestcontroller.ControlPoint;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.ee.concurrent.SecurityIdentityUtils.doIdentityWrap;

/**
 * @author Stuart Douglas
 * @author emmartins
 */
public class ConcurroManagedExecutorServiceImpl extends org.glassfish.concurro.ManagedExecutorServiceImpl implements WildFlyManagedExecutorService {

    private final ControlPoint controlPoint;
    private final ProcessStateNotifier processStateNotifier;
    private final ManagedExecutorRuntimeStats runtimeStats;

    public ConcurroManagedExecutorServiceImpl(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        super(name, (ConcurroManagedThreadFactoryImpl) managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, (ContextServiceImpl) contextService, convertRejectPolicy(rejectPolicy), queue);
        this.controlPoint = controlPoint;
        this.processStateNotifier = processStateNotifier;
        this.runtimeStats = new ConcurroManagedExecutorRuntimeStatsImpl(this);
    }

    public ConcurroManagedExecutorServiceImpl(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        super(name, (ConcurroManagedThreadFactoryImpl) managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, (ContextServiceImpl) contextService, convertRejectPolicy(rejectPolicy));
        this.controlPoint = controlPoint;
        this.processStateNotifier = processStateNotifier;
        this.runtimeStats = new ConcurroManagedExecutorRuntimeStatsImpl(this);
    }

    public static AbstractManagedExecutorService.RejectPolicy convertRejectPolicy(WildFlyManagedExecutorService.RejectPolicy rejectPolicy) {
        return rejectPolicy != null ? AbstractManagedExecutorService.RejectPolicy.valueOf(rejectPolicy.toString()) : null;
    }

    @Override
    public WildFlyManagedThreadFactory getWildFlyManagedThreadFactory() {
        return (WildFlyManagedThreadFactory) getManagedThreadFactory();
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        final Callable<T> callable = ControlPointUtils.doWrap(task, controlPoint, processStateNotifier);
        try {
            return super.submit(doIdentityWrap(callable));
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw e;
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        final Runnable runnable = ControlPointUtils.doWrap(task, controlPoint, processStateNotifier);
        try {
            return super.submit(doIdentityWrap(runnable), result);
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw e;
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        final Runnable runnable = ControlPointUtils.doWrap(task, controlPoint, processStateNotifier);
        try {
            return super.submit(doIdentityWrap(runnable));
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw e;
        }
    }

    @Override
    public void execute(Runnable command) {
        final Runnable runnable = ControlPointUtils.doWrap(command, controlPoint, processStateNotifier);
        try {
            super.execute(doIdentityWrap(runnable));
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw e;
        }
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
