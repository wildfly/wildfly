/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.thread.pool;

import org.jberet.spi.JobExecutor;
import org.jboss.as.threads.ManagedJBossThreadPoolExecutorService;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JobExecutorService implements Service {

    private final Consumer<JobExecutor> jobExecutorConsumer;
    private final Supplier<ManagedJBossThreadPoolExecutorService> threadPoolSupplier;

    public JobExecutorService(final Consumer<JobExecutor> jobExecutorConsumer,
                              final Supplier<ManagedJBossThreadPoolExecutorService> threadPoolSupplier) {
        this.jobExecutorConsumer = jobExecutorConsumer;
        this.threadPoolSupplier = threadPoolSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        jobExecutorConsumer.accept(new WildFlyJobExecutor(threadPoolSupplier.get()));
    }

    @Override
    public void stop(final StopContext context) {
        jobExecutorConsumer.accept(null);
    }

}
