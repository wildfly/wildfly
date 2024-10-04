/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.controller.ProcessStateNotifier;
import org.wildfly.extension.requestcontroller.ControlPoint;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.ee.concurrent.ControlPointUtils.doWrap;
import static org.jboss.as.ee.concurrent.SecurityIdentityUtils.doIdentityWrap;

/**
 * @author Stuart Douglas
 * @author emmartins
 */
public class ManagedExecutorServiceImpl extends org.glassfish.enterprise.concurrent.ManagedExecutorServiceImpl implements ManagedExecutorWithHungThreads {

    private final ControlPoint controlPoint;
    private final ProcessStateNotifier processStateNotifier;
    private final ManagedExecutorRuntimeStats runtimeStats;

    public ManagedExecutorServiceImpl(String name, ManagedThreadFactoryImpl managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, ContextServiceImpl contextService, RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, queue);
        this.controlPoint = controlPoint;
        this.processStateNotifier = processStateNotifier;
        this.runtimeStats = new ManagedExecutorRuntimeStatsImpl(this);
    }

    public ManagedExecutorServiceImpl(String name, ManagedThreadFactoryImpl managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, ContextServiceImpl contextService, RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, contextService, rejectPolicy);
        this.controlPoint = controlPoint;
        this.processStateNotifier = processStateNotifier;
        this.runtimeStats = new ManagedExecutorRuntimeStatsImpl(this);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        final Callable<T> callable = doWrap(task, controlPoint, processStateNotifier);
        try {
            return super.submit(doIdentityWrap(callable));
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw e;
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        final Runnable runnable = doWrap(task, controlPoint, processStateNotifier);
        try {
            return super.submit(doIdentityWrap(runnable), result);
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw e;
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        final Runnable runnable = doWrap(task, controlPoint, processStateNotifier);
        try {
            return super.submit(doIdentityWrap(runnable));
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw e;
        }
    }

    @Override
    public void execute(Runnable command) {
        final Runnable runnable = doWrap(command, controlPoint, processStateNotifier);
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
}
