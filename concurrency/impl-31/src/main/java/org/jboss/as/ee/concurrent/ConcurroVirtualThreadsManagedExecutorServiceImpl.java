/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import static org.jboss.as.ee.concurrent.SecurityIdentityUtils.doIdentityWrap;
import org.glassfish.concurro.AbstractManagedExecutorService;
import org.glassfish.concurro.ContextServiceImpl;
import org.glassfish.concurro.virtualthreads.VirtualThreadsManagedThreadFactory;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.extension.requestcontroller.ControlPoint;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * WildFly's extension of {@link org.glassfish.concurro.virtualthreads.VirtualThreadsManagedExecutorService}.
 * @author Eduardo Martins
 */
public class ConcurroVirtualThreadsManagedExecutorServiceImpl extends org.glassfish.concurro.virtualthreads.VirtualThreadsManagedExecutorService implements WildFlyManagedExecutorService {

    private final ControlPoint controlPoint;
    private final ProcessStateNotifier processStateNotifier;
    private final ManagedExecutorRuntimeStats runtimeStats = new RuntimeStats();

    public ConcurroVirtualThreadsManagedExecutorServiceImpl(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int maxPoolSize, int queueCapacity, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        super(name, (VirtualThreadsManagedThreadFactory) managedThreadFactory, hungTaskThreshold, longRunningTasks, maxPoolSize, queueCapacity, (ContextServiceImpl) contextService, convertRejectPolicy(rejectPolicy));
        this.controlPoint = controlPoint;
        this.processStateNotifier = processStateNotifier;
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

                final String taskIdentityName = t.toString();
                try {
                    t.interrupt();
                    EeLogger.ROOT_LOGGER.hungTaskCancelled(executorName, taskIdentityName);
                } catch (Throwable throwable) {
                    EeLogger.ROOT_LOGGER.huntTaskTerminationFailure(throwable, executorName, taskIdentityName);
                }
            }
        }
    }

    public class RuntimeStats implements ManagedExecutorRuntimeStats {

        @Override
        public int getActiveThreadsCount() {
            return getThreadsCount();
        }

        @Override
        public long getCompletedTaskCount() {
            return ConcurroVirtualThreadsManagedExecutorServiceImpl.super.getCompletedTaskCount();
        }

        @Override
        public int getHungThreadsCount() {
            return ConcurroVirtualThreadsManagedExecutorServiceImpl.super.getHungThreads().size();
        }

        @Override
        public int getMaxThreadsCount() {
            return 0;
        }

        @Override
        public int getQueueSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long getTaskCount() {
            return ConcurroVirtualThreadsManagedExecutorServiceImpl.super.getTaskCount();
        }

        @Override
        public int getThreadsCount() {
            return ConcurroVirtualThreadsManagedExecutorServiceImpl.super.getThreads().size();
        }
    }
}
