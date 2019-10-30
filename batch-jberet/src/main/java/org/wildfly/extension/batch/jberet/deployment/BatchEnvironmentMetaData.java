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
    private final String executorName;
    private final Boolean restartJobsOnResume;

    protected BatchEnvironmentMetaData(final JobRepository jobRepository, final String jobRepositoryName, final String executorName, final Boolean restartJobsOnResume) {
        this.jobRepository = jobRepository;
        this.jobRepositoryName = jobRepositoryName;
        this.executorName = executorName;
        this.restartJobsOnResume = restartJobsOnResume;
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }

    public String getJobRepositoryName() {
        return jobRepositoryName;
    }

    public String getExecutorName() {
        return executorName;
    }

    public Boolean getRestartJobsOnResume() {
        return restartJobsOnResume;
    }
}
