/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.concurro.AbstractManagedExecutorService;

import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executor runtime stats obtained from a thread pool executor.
 * @author emmartins
 */
class ConcurroManagedExecutorRuntimeStatsImpl implements ManagedExecutorRuntimeStats {

    private final AbstractManagedExecutorService abstractManagedExecutorService;
    private final ThreadPoolExecutor threadPoolExecutor;

    ConcurroManagedExecutorRuntimeStatsImpl(ConcurroManagedExecutorServiceImpl executorService) {
        this.abstractManagedExecutorService = executorService;
        this.threadPoolExecutor = executorService.getThreadPoolExecutor();
    }

    ConcurroManagedExecutorRuntimeStatsImpl(ConcurroManagedScheduledExecutorServiceImpl executorService) {
        this.abstractManagedExecutorService = executorService;
        this.threadPoolExecutor = executorService.getThreadPoolExecutor();
    }

    @Override
    public int getThreadsCount() {
        return threadPoolExecutor.getPoolSize();
    }

    @Override
    public int getActiveThreadsCount() {
        return threadPoolExecutor.getActiveCount();
    }

    @Override
    public int getMaxThreadsCount() {
        return threadPoolExecutor.getLargestPoolSize();
    }

    @Override
    public int getHungThreadsCount() {
        final Collection hungThreads = abstractManagedExecutorService.getHungThreads();
        return hungThreads != null ? hungThreads.size() : 0;
    }

    @Override
    public long getTaskCount() {
        return threadPoolExecutor.getTaskCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return threadPoolExecutor.getCompletedTaskCount();
    }

    @Override
    public int getQueueSize() {
        return threadPoolExecutor.getQueue().size();
    }
}
