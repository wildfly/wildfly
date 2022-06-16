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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.CountingItemReader;
import org.jboss.as.test.integration.batch.common.CountingItemWriter;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import java.util.List;
import java.util.Properties;

@RunWith(Arquillian.class)
public class InMemoryRepositoryTestCase extends AbstractBatchTestCase {

    private static final String DEPLOYMENT_NAME = "jdbc-batch.war";

    @ArquillianResource
    private ServiceContainer serviceContainer;

    @Deployment(name = DEPLOYMENT_NAME)
    public static WebArchive createNamedJdbcDeployment() {
        return createDefaultWar(DEPLOYMENT_NAME, InMemoryRepositoryTestCase.class.getPackage(), "test-chunk.xml")
                .addClasses(CountingItemReader.class, CountingItemWriter.class, JobRepositoryTestUtils.class)
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.msc,org.wildfly.security.manager,org.wildfly.extension.batch.jberet")
                                .exportAsString()));
    }

    @Test
    public void testGetJobExecutions() {
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

        ServiceController<?> service = serviceContainer.getService(ServiceName.of("org", "wildfly", "batch", "job", "repository", "in-memory"));
        JobRepository repositoryService = (JobRepository) service.getValue();
        List<Long> list = repositoryService.getJobExecutionsByJob("test-chunk");
        Assert.assertEquals(5, list.size());
        // executions should be retrieved in the order from latest to the oldest
        Assert.assertEquals(latestExecutionId, (long) list.get(0));
        Assert.assertEquals(latestExecutionId - 1, (long) list.get(1));
        Assert.assertEquals(latestExecutionId - 2, (long) list.get(2));
        Assert.assertEquals(latestExecutionId - 3, (long) list.get(3));
        Assert.assertEquals(latestExecutionId - 4, (long) list.get(4));
    }

}
