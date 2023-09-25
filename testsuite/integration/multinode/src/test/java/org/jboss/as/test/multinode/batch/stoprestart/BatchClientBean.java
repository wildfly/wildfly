/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.batch.stoprestart;

import java.util.Properties;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BatchClientBean implements BatchClientIF {
    private final JobOperator jobOperator = BatchRuntime.getJobOperator();

    @Override
    public long start(String jobName, Properties jobParams) {
        return jobOperator.start(jobName, jobParams);
    }

    @Override
    public void stop(long jobExecutionId) {
        jobOperator.stop(jobExecutionId);
    }

    @Override
    public long restart(long jobExecutionId, Properties restartParams) {
        return jobOperator.restart(jobExecutionId, restartParams);
    }

    @Override
    public BatchStatus getJobStatus(long jobExecutionId) {
        final JobExecution jobExecution = jobOperator.getJobExecution(jobExecutionId);
        return jobExecution.getBatchStatus();
    }
}
