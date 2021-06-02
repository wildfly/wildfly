/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.multinode.batch.stoprestart;

import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

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
