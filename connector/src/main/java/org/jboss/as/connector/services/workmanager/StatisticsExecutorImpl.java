/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager;

import java.util.concurrent.Executor;

import org.jboss.as.threads.ManagedJBossThreadPoolExecutorService;
import org.jboss.as.threads.ManagedQueueExecutorService;
import org.jboss.as.threads.ManagedQueuelessExecutorService;
import org.jboss.as.threads.ManagedScheduledExecutorService;
import org.jboss.jca.core.api.workmanager.StatisticsExecutor;
import org.jboss.threads.JBossThreadPoolExecutor;
import org.jboss.threads.management.ThreadPoolExecutorMBean;

/**
 * A StatisticsExecutor implementation keeping track of numberOfFreeThreads
 *
 * @author Stefano Maestri
 */

public class StatisticsExecutorImpl implements StatisticsExecutor {

    private final Executor realExecutor;

    /**
     * StatisticsExecutorImpl constructor
     *
     * @param realExecutor the real executor we are delegating
     */
    public StatisticsExecutorImpl(Executor realExecutor) {
        this.realExecutor = realExecutor;
    }


    @Override
    public void execute(Runnable runnable) {
        realExecutor.execute(runnable);
    }

    @Override
    public long getNumberOfFreeThreads() {
        if (realExecutor instanceof JBossThreadPoolExecutor) {
            return (long) ((JBossThreadPoolExecutor) realExecutor).getMaximumPoolSize()
                    - ((JBossThreadPoolExecutor) realExecutor).getActiveCount();
        }
        if (realExecutor instanceof ThreadPoolExecutorMBean) {
            return (long) ((ThreadPoolExecutorMBean) realExecutor).getMaxThreads()
                    - ((ThreadPoolExecutorMBean) realExecutor).getCurrentThreadCount();
        }
        if (realExecutor instanceof ManagedQueueExecutorService) {
            return (long) ((ManagedQueueExecutorService) realExecutor).getMaxThreads()
                    - ((ManagedQueueExecutorService) realExecutor).getCurrentThreadCount();
        }
        if (realExecutor instanceof ManagedJBossThreadPoolExecutorService) {
            return (long) ((ManagedJBossThreadPoolExecutorService) realExecutor).getMaxThreads()
                    - ((ManagedJBossThreadPoolExecutorService) realExecutor).getCurrentThreadCount();
        }
        if (realExecutor instanceof ManagedQueuelessExecutorService) {
            return (long) ((ManagedQueuelessExecutorService) realExecutor).getMaxThreads()
                    - ((ManagedQueuelessExecutorService) realExecutor).getCurrentThreadCount();
        }
        if (realExecutor instanceof ManagedScheduledExecutorService) {
            return (long) ((ManagedScheduledExecutorService) realExecutor).getLargestPoolSize()
                    - ((ManagedScheduledExecutorService) realExecutor).getActiveCount();
        }
        return 0L;
    }
}
