/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.suspend;

import java.io.FilePermission;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that starts a long-running Batchlet and performs a suspend/resume.
 * The batch should be restarted after the resume.
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
public class SuspendBatchletTestCase extends AbstractBatchTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive createDeployment() {
        return createDefaultWar("suspend-batchlet.war", SuspendBatchletTestCase.class.getPackage(), "suspend-batchlet.xml")
                .addClass(LongRunningBatchlet.class)
                .addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller, org.jboss.remoting\n"), "META-INF/MANIFEST.MF")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new RemotingPermission("createEndpoint"),
                        new RemotingPermission("connect"),
                        new PropertyPermission("ts.timeout.factor", "read"),
                        new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
                ), "permissions.xml");
    }

    private void suspendServer() throws IOException {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("suspend");
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertTrue("Failed to suspend: " + result, Operations.isSuccessfulOutcome(result));
    }

    private void resumeServer() throws IOException {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("resume");
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertTrue("Failed to resume: " + result, Operations.isSuccessfulOutcome(result));
    }

    private void checkJobExecution(JobOperator jobOperator, JobExecution jobExecution, BatchStatus expectedBatchStatus, String expectedExitStatus) {
        waitForTermination(jobExecution, 10);
        Assert.assertEquals("The batchlet is not in the expected batch status", expectedBatchStatus, jobExecution.getBatchStatus());
        List<StepExecution> steps = jobOperator.getStepExecutions(jobExecution.getExecutionId());
        Assert.assertFalse("The job execution has no steps", steps.isEmpty());
        Assert.assertEquals("The batchlet did not return the expected exit status", expectedExitStatus, steps.get(0).getExitStatus());
    }

    // Reimplementation of the following if we ever decide to simplify our testing using Awaitility:
    // Awaitility.await("batchlet to start executing").atMost(TimeoutUtil.adjust(10), TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).until(LongRunningBatchlet::isStarted);
    // Alternatively also be generalized with SuspendBatchletTestCase#waitForJobRestarted
    private static void waitForBatchletToStart(final int timeoutInSeconds) {
        long timeoutMillis = TimeoutUtil.adjust(timeoutInSeconds * 1000); // Convert seconds to milliseconds
        long deadline = System.currentTimeMillis() + timeoutMillis;
        long pollIntervalMillis = 100L; // Use a fixed interval for simplicity

        while (System.currentTimeMillis() < deadline) {
            if (LongRunningBatchlet.isStarted()) {
                return;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait for batchlet start interrupted", e);
            }
        }

        throw new IllegalStateException("Batchlet did not start executing within the allotted time of " + timeoutInSeconds + " seconds.");
    }

    private static JobExecution waitForJobRestarted(final JobOperator jobOperator, final JobInstance jobInstance, final int timeout) {
        long waitTimeout = TimeoutUtil.adjust(timeout * 1000);
        long sleep = 100L;
        JobExecution jobExecution = null;
        while (jobExecution == null) {
            for (JobExecution je : jobOperator.getJobExecutions(jobInstance)) {
                if (je.getBatchStatus() == BatchStatus.STARTED && LongRunningBatchlet.isStarted()) {
                    jobExecution = je;
                    break;
                }
            }
            if (jobExecution == null) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleep);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                waitTimeout -= sleep;
                if (waitTimeout <= 0) {
                    throw new IllegalStateException("Batch job was not restarted within the allotted time.");
                }
            }
        }
        return jobExecution;
    }

    /**
     * Tests that a batchlet is restarted after a server suspend/resume.
     * @throws Exception if an error occurs
     */
    @Test
    public void testSuspendResume() throws Exception {
        final Properties jobProperties = new Properties();
        jobProperties.setProperty("max.seconds", "10");
        final JobOperator jobOperator = BatchRuntime.getJobOperator();

        long executionId = jobOperator.start("suspend-batchlet", jobProperties);
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        // Wait for the batchlet to actually start executing before suspending (WFLY-16653)
        // JobOperator.start() is asynchronous, so we need to wait for the batchlet's
        // process() method to be called before suspending the server
        waitForBatchletToStart(10);

        suspendServer();

        // check job is stopped
        checkJobExecution(jobOperator, jobExecution, BatchStatus.STOPPED, "KO");

        resumeServer();

        // the job should be restarted with a new ID, wait for it a max of 10s
        JobInstance jobInstance = jobOperator.getJobInstance(executionId);
        jobExecution = waitForJobRestarted(jobOperator, jobInstance, 10);

        // hack to force the batchlet to finish now it's finally started
        LongRunningBatchlet.success();

        // check job finishes OK
        checkJobExecution(jobOperator, jobExecution, BatchStatus.COMPLETED, "OK");
    }
}
