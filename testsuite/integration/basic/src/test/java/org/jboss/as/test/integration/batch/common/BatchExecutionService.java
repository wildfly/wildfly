/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.common;

import java.util.Properties;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.JobExecution;
import jakarta.inject.Singleton;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Singleton
public class BatchExecutionService {

    private final JobOperator jobOperator;

    public BatchExecutionService() {
        jobOperator = BatchRuntime.getJobOperator();
    }

    public JobOperator getJobOperator() {
        return jobOperator;
    }

    public JobExecution start(final String jobXml, final Properties params) {
        final long id = jobOperator.start(jobXml, params);
        return jobOperator.getJobExecution(id);
    }
}
