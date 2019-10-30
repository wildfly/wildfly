/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2010, Red Hat Inc, and individual contributors
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

package org.jboss.as.connector.services.workmanager;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.jboss.as.threads.ManagedJBossThreadPoolExecutorService;
import org.jboss.as.threads.ManagedQueueExecutorService;
import org.jboss.as.threads.ManagedQueuelessExecutorService;
import org.jboss.as.threads.ManagedScheduledExecutorService;
import org.jboss.jca.core.CoreLogger;
import org.jboss.jca.core.api.workmanager.StatisticsExecutor;
import org.jboss.logging.Logger;
import org.jboss.threads.BlockingExecutor;
import org.jboss.threads.JBossThreadPoolExecutor;
import org.jboss.threads.management.ThreadPoolExecutorMBean;

/**
 * A StatisticsExecutor implementation keeping track of numberOfFreeThreads
 *
 * @author Stefano Maestri
 */

public class StatisticsExecutorImpl implements StatisticsExecutor {
    /**
     * The logger
     */
    private static CoreLogger log = Logger.getMessageLogger(CoreLogger.class,
            org.jboss.jca.core.workmanager.StatisticsExecutorImpl.class.getName());

    private final BlockingExecutor realExecutor;

    /**
     * StatisticsExecutorImpl constructor
     *
     * @param realExecutor the real executor we are delegating
     */
    public StatisticsExecutorImpl(BlockingExecutor realExecutor) {
        this.realExecutor = realExecutor;
    }


    @Override
    public void execute(Runnable runnable) {
        realExecutor.execute(runnable);
    }

    @Override
    public void executeBlocking(Runnable runnable) throws RejectedExecutionException, InterruptedException {
        realExecutor.executeBlocking(runnable);
    }

    @Override
    public void executeBlocking(Runnable runnable, long l, TimeUnit timeUnit) throws RejectedExecutionException,
            InterruptedException {
        realExecutor.executeBlocking(runnable, l, timeUnit);
    }

    @Override
    public void executeNonBlocking(Runnable runnable) throws RejectedExecutionException {
        realExecutor.executeNonBlocking(runnable);
    }

    @Override
    public long getNumberOfFreeThreads() {
        if (realExecutor instanceof JBossThreadPoolExecutor) {
            return ((JBossThreadPoolExecutor) realExecutor).getMaximumPoolSize() -
                    ((JBossThreadPoolExecutor) realExecutor).getActiveCount();
        } else if (realExecutor instanceof ThreadPoolExecutorMBean) {
            return ((ThreadPoolExecutorMBean) realExecutor).getMaxThreads() -
                    ((ThreadPoolExecutorMBean) realExecutor).getCurrentThreadCount();
        } else if (realExecutor instanceof ManagedQueueExecutorService) {
            return ((ManagedQueueExecutorService) realExecutor).getMaxThreads() -
                    ((ManagedQueueExecutorService) realExecutor).getCurrentThreadCount();
        } else if (realExecutor instanceof ManagedJBossThreadPoolExecutorService) {
            return ((ManagedJBossThreadPoolExecutorService) realExecutor).getMaxThreads() -
                    ((ManagedJBossThreadPoolExecutorService) realExecutor).getCurrentThreadCount();
        } else if (realExecutor instanceof ManagedQueuelessExecutorService) {
            return ((ManagedQueuelessExecutorService) realExecutor).getMaxThreads() -
                    ((ManagedQueuelessExecutorService) realExecutor).getCurrentThreadCount();
        } else if (realExecutor instanceof ManagedScheduledExecutorService) {
            return ((ManagedScheduledExecutorService) realExecutor).getLargestPoolSize() -
                    ((ManagedScheduledExecutorService) realExecutor).getActiveCount();
        } else {
            return 0;
        }

    }
}
