/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;

import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executor runtime stats obtained from a thread pool executor.
 * @author emmartins
 */
class ManagedExecutorRuntimeStatsImpl implements ManagedExecutorRuntimeStats {

    private final AbstractManagedExecutorService abstractManagedExecutorService;
    private final ThreadPoolExecutor threadPoolExecutor;

    ManagedExecutorRuntimeStatsImpl(ManagedExecutorServiceImpl executorService) {
        this.abstractManagedExecutorService = executorService;
        this.threadPoolExecutor = executorService.getThreadPoolExecutor();
    }

    ManagedExecutorRuntimeStatsImpl(ManagedScheduledExecutorServiceImpl executorService) {
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
