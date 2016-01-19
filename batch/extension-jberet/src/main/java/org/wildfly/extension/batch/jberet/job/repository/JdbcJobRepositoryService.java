/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet.job.repository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import javax.sql.DataSource;

import org.jberet.repository.JdbcRepository;
import org.jberet.repository.JobRepository;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * A service which provides a JDBC job repository.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JdbcJobRepositoryService extends JobRepositoryService implements Service<JobRepository> {

    private final InjectedValue<DataSource> dataSourceValue = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executor = new InjectedValue<>();
    private volatile JdbcRepository jobRepository;

    @Override
    public void startJobRepository(final StartContext context) throws StartException {
        final ExecutorService service = executor.getValue();
        final Runnable task = () -> {
            try {
                // Currently in jBeret tables are created in the constructor which is why this is done asynchronously
                jobRepository = new JdbcRepository(dataSourceValue.getValue());
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

    protected InjectedValue<DataSource> getDataSourceInjector() {
        return dataSourceValue;
    }

    protected Injector<ExecutorService> getExecutorServiceInjector() {
        return executor;
    }
}
