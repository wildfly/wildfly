/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.thread.pool;

import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobTask;
import org.jboss.as.threads.ManagedJBossThreadPoolExecutorService;
import org.wildfly.extension.requestcontroller.ControlPointTask;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class WildFlyJobExecutor extends JobExecutor {
    private final ManagedJBossThreadPoolExecutorService delegate;

    public WildFlyJobExecutor(final ManagedJBossThreadPoolExecutorService delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    protected int getMaximumPoolSize() {
        return delegate.getMaxThreads();
    }

    @Override
    protected JobTask wrap(final Runnable task) {
        if (task instanceof JobTask) {
            return (JobTask) task;
        }

        final int requiredRemaining;
        if (task instanceof ControlPointTask) {
            final Runnable originalTask = ((ControlPointTask) task).getOriginalTask();
            if (originalTask instanceof JobTask) {
                requiredRemaining = ((JobTask) originalTask).getRequiredRemainingPermits();
            } else {
                requiredRemaining = 0;
            }
        } else {
            requiredRemaining = 0;
        }

        return new JobTask() {
            @Override
            public int getRequiredRemainingPermits() {
                return requiredRemaining;
            }

            @Override
            public void run() {
                task.run();
            }
        };
    }
}
