/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.job.repository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;

import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * A service which provides a JDBC job repository.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JdbcJobRepositoryService extends JobRepositoryService implements Service<JobRepository> {

    private final Supplier<DataSource> dataSourceSupplier;
    private final Supplier<ExecutorService> executorSupplier;
    private volatile JdbcRepository jobRepository;

    public JdbcJobRepositoryService(final Consumer<JobRepository> jobRepositoryConsumer,
                                    final Supplier<DataSource> dataSourceSupplier,
                                    final Supplier<ExecutorService> executorSupplier,
                                    final Integer executionRecordsLimit) {
        super(jobRepositoryConsumer, executionRecordsLimit);
        this.dataSourceSupplier = dataSourceSupplier;
        this.executorSupplier = executorSupplier;
    }

    @Override
    public void startJobRepository(final StartContext context) throws StartException {
        final ExecutorService service = executorSupplier.get();
        final Runnable task = () -> {
            try {
                // Currently in jBeret tables are created in the constructor which is why this is done asynchronously
                jobRepository = new JdbcRepository(dataSourceSupplier.get());
                context.complete();
            } catch (Exception e) {
                context.failed(BatchLogger.LOGGER.failedToCreateJobRepository(e, "JDBC"));
            }
        };
        try {
            service.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public void stopJobRepository(final StopContext context) {
        jobRepository = null;
    }

    @Override
    protected JobRepository getDelegate() {
        return jobRepository;
    }
}
