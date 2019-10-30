/*
 * Copyright 2017 Red Hat, Inc.
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

package org.jboss.as.test.integration.batch.batchlet;

import java.util.List;
import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a {@link javax.faces.bean.RequestScoped @RequestScoped} bean will work correctly in a batch job.
 * <p>
 * The order of the two tests should not matter. The reason it's tested twice however is a {@code @RequestScoped} bean
 * should create a new instance for each job executed. Executing twice ensures the scoping for batch is correct.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
public class SimpleCounterBatchletTestCase extends AbstractBatchTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        return createDefaultWar("simple-counter-batchlet.war", SimpleCounterBatchletTestCase.class.getPackage(), "simple-counter-batchlet.xml")
                .addPackage(SimpleCounterBatchletTestCase.class.getPackage());

    }

    /**
     * Tests that a request scope bean will be injected and the counter should start with 1 and end at 10.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testRequestScope10() throws Exception {
        final Properties jobProperties = new Properties();
        jobProperties.setProperty("count", "10");
        final JobOperator jobOperator = BatchRuntime.getJobOperator();

        // Start the first job
        long executionId = jobOperator.start("simple-counter-batchlet", jobProperties);

        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        // Wait until the job is complete for a maximum of 5 seconds
        waitForTermination(jobExecution, 5);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        // Get the first step, should only be one, and get the exit status
        Assert.assertEquals("1,2,3,4,5,6,7,8,9,10", getFirst(jobOperator, executionId).getExitStatus());
    }


    /**
     * Tests that a request scope bean will be injected and the counter should start with 1 and end at 8.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testRequestScope8() throws Exception {
        final Properties jobProperties = new Properties();
        jobProperties.setProperty("count", "8");
        final JobOperator jobOperator = BatchRuntime.getJobOperator();

        // Start the first job
        long executionId = jobOperator.start("simple-counter-batchlet", jobProperties);

        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        // Wait until the job is complete for a maximum of 5 seconds
        waitForTermination(jobExecution, 5);
        Assert.assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

        // Get the first step, should only be one, and get the exit status
        Assert.assertEquals("1,2,3,4,5,6,7,8", getFirst(jobOperator, executionId).getExitStatus());
    }

    private StepExecution getFirst(final JobOperator jobOperator, final long executionId) {
        final List<StepExecution> steps = jobOperator.getStepExecutions(executionId);
        Assert.assertFalse(steps.isEmpty());
        return steps.get(0);
    }
}
