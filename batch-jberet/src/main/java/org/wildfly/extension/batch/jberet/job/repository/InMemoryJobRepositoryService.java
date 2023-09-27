/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.job.repository;

import org.jberet.repository.InMemoryRepository;
import org.jberet.repository.JobRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;

/**
 * A service which provides an in-memory job repository.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class InMemoryJobRepositoryService extends JobRepositoryService implements Service<JobRepository> {

    private volatile InMemoryRepository repository;

    public InMemoryJobRepositoryService(final Consumer<JobRepository> jobRepositoryConsumer, final Integer executionRecordsLimit) {
        super(jobRepositoryConsumer, executionRecordsLimit);
    }

    @Override
    public void startJobRepository(final StartContext context) throws StartException {
        repository = new InMemoryRepository();
    }

    @Override
    public void stopJobRepository(final StopContext context) {
        repository = null;
    }

    @Override
    protected JobRepository getDelegate() {
        return repository;
    }
}
