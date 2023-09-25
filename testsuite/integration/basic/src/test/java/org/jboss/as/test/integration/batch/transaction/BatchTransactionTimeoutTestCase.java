/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.transaction;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import jakarta.batch.runtime.JobExecution;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.JobExecutionMarshaller;
import org.jboss.as.test.integration.batch.common.StartBatchServlet;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for JBERET-231/JBEAP-4811.
 * When a batch job fails due to transaction timeout, another job should be able to run on the same thread.
 * <p>
 * {@link ThreadBatchSetup} configures jberet to have {@code max-threads} 3.
 * The test job, {@code timeout-job.xml}, contains a step with 2 partitions.
 * The test starts the job twice: first with a {@code job.timeout} value to trigger transaction timeout
 * and job execution failure; second time without causing transaction timeout.
 * <p>
 * The first job execution is expected to fail due to transaction timeout.
 * The second job execution should complete successfully.
 */
@RunWith(Arquillian.class)
@ServerSetup(ThreadBatchSetup.class)
public class BatchTransactionTimeoutTestCase extends AbstractBatchTestCase {

    @Deployment
    public static WebArchive createDeployment() {
        return createDefaultWar("batch-transaction-timeout.war", BatchTransactionTimeoutTestCase.class.getPackage(), "timeout-job.xml")
                .addPackage(BatchTransactionTimeoutTestCase.class.getPackage())
                .addAsResource("org/jboss/as/test/integration/batch/transaction/persistence.xml", "META-INF/persistence.xml");
    }

    @RunAsClient
    @Test
    public void testThreadIsAvailableForNextJob(@ArquillianResource final URL url) throws Exception {
        assertEquals("FAILED", executeJobWithTimeout(url));

        assertEquals("COMPLETED", executeJobWithoutTimout(url));
    }

    private String executeJobWithTimeout(@ArquillianResource URL url) throws ExecutionException, IOException, TimeoutException {
        return executeJob(url, 10000);
    }

    private String executeJobWithoutTimout(@ArquillianResource URL url) throws ExecutionException, IOException, TimeoutException {
        return executeJob(url, -1);
    }

    private String executeJob(@ArquillianResource URL url, int timeout) throws ExecutionException, IOException, TimeoutException {
        final UrlBuilder builder = UrlBuilder.of(url, "start");
        builder.addParameter(StartBatchServlet.JOB_XML_PARAMETER, "timeout-job");
        builder.addParameter("job.timeout", timeout);

        final String result = performCall(builder.build(), 20);
        final JobExecution jobExecution = JobExecutionMarshaller.unmarshall(result);

        return jobExecution.getExitStatus();
    }
}
