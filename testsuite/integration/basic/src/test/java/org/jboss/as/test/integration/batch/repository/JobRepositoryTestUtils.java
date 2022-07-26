/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.batch.repository;

import org.jberet.repository.JobRepository;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public final class JobRepositoryTestUtils {

    public static void testGetJobExecutionsWithLimit(ServiceContainer serviceContainer, String repositoryName) {
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final Properties jobProperties = new Properties();
        jobProperties.setProperty("reader.end", "10");
        jobProperties.setProperty("writer.sleep.time", "0");

        long latestExecutionId = 0;
        for (int i = 0; i < 5; i++) {
            // Start the first job
            latestExecutionId = jobOperator.start("test-chunk", jobProperties);
            JobExecution jobExecution = jobOperator.getJobExecution(latestExecutionId);
            // Wait until the job is complete for a maximum of 5 seconds
            AbstractBatchTestCase.waitForTermination(jobExecution, 5);
            // Check the job as completed and the expected execution id should be 1
            Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
        }

        serviceContainer.dumpServices();

        ServiceController<?> service = serviceContainer.getService(ServiceName.of("org", "wildfly", "batch", "job", "repository", repositoryName));
        Assert.assertNotNull(String.format("MSC service for the %s job repository not found. Services found: %s",
                repositoryName,
                serviceContainer.getServiceNames().stream()
                        .map(ServiceName::toString)
                        .filter(n -> n.contains("jdbc"))
                        .collect(Collectors.joining("\n"))), service);
        JobRepository repositoryService = (JobRepository) service.getValue();
        List<Long> list = repositoryService.getJobExecutionsByJob("test-chunk");
        Assert.assertEquals(2, list.size());
        // the last two executions are supposed to be obtained
        Assert.assertEquals(latestExecutionId, (long) list.get(0));
        Assert.assertEquals(latestExecutionId - 1, (long) list.get(1));

        // override the limit and verify that all executions were retrieved
        list = repositoryService.getJobExecutionsByJob("test-chunk", 10);
        Assert.assertEquals(5, list.size());
    }

}
