/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jberet.repository.JobRepository;

/**
 * Represents environment objects created via a deployment descriptor.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class BatchEnvironmentMetaData {
    private final JobRepository jobRepository;
    private final String jobRepositoryName;
    private final String dataSourceName;
    private final String executorName;
    private final Boolean restartJobsOnResume;
    private final Integer executionRecordsLimit;

    protected BatchEnvironmentMetaData(final JobRepository jobRepository,
                                       final String jobRepositoryName,
                                       final String dataSourceName,
                                       final String executorName,
                                       final Boolean restartJobsOnResume,
                                       final Integer executionRecordsLimit) {
        this.jobRepository = jobRepository;
        this.jobRepositoryName = jobRepositoryName;
        this.dataSourceName = dataSourceName;
        this.executorName = executorName;
        this.restartJobsOnResume = restartJobsOnResume;
        this.executionRecordsLimit = executionRecordsLimit;
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }

    public String getJobRepositoryName() {
        return jobRepositoryName;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getExecutorName() {
        return executorName;
    }

    public Boolean getRestartJobsOnResume() {
        return restartJobsOnResume;
    }

    public Integer getExecutionRecordsLimit() {
        return executionRecordsLimit;
    }
}
