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

package org.jboss.as.test.integration.batch.deployment;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.io.IOException;
import java.util.PropertyPermission;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.CountingItemReader;
import org.jboss.as.test.integration.batch.common.CountingItemWriter;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the start, stop and restart functionality for deployments.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(JobControlTestCase.DebugLoggingSetup.class)
public class JobControlTestCase extends AbstractBatchTestCase {

    private static final String DEPLOYMENT_NAME = "test-batch.war";

    @ArquillianResource
    private ManagementClient managementClient;

    @Inject
    private CountingItemWriter countingItemWriter;

    private int currentCount = 0;

    @Deployment(name = DEPLOYMENT_NAME)
    public static WebArchive createNamedInMemoryDeployment() {
        return createDefaultWar(DEPLOYMENT_NAME, DeploymentDescriptorTestCase.class.getPackage(), "test-chunk.xml")
                .addClasses(CountingItemReader.class, CountingItemWriter.class)
                .addClass(Operations.class)
                .addAsResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller, org.jboss.remoting3\n"), "META-INF/MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new RemotingPermission("createEndpoint"),
                        new RemotingPermission("connect"),
                        new PropertyPermission("ts.timeout.factor", "read"),
                        new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
                ), "permissions.xml");
    }

    @Test
    public void testStart() throws Exception {
        final ModelNode address = Operations.createAddress("deployment", DEPLOYMENT_NAME, "subsystem", "batch-jberet");
        final ModelNode op = Operations.createOperation("start-job", address);
        op.get("job-xml-name").set("test-chunk");
        final ModelNode properties = op.get("properties");
        properties.get("reader.end").set("5");
        final ModelNode result = executeOperation(op);
        currentCount += 5;
        final long executionId = result.asLong();
        Assert.assertTrue("Execution id should be greater than 0", executionId > 0L);

        // Validate that the job as executed
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final JobExecution execution = jobOperator.getJobExecution(executionId);
        Assert.assertNotNull(execution);

        // Wait for 3 seconds max for the execution to finish.
        waitForTermination(execution, 3);

        // Check that we have 5 items
        Assert.assertEquals(currentCount, countingItemWriter.getWrittenItemSize());
    }

    @Test
    public void testStop() throws Exception {
        final ModelNode address = Operations.createAddress("deployment", DEPLOYMENT_NAME, "subsystem", "batch-jberet");
        ModelNode op = Operations.createOperation("start-job", address);
        op.get("job-xml-name").set("test-chunk");
        final ModelNode properties = op.get("properties");
        properties.get("reader.end").set("20");
        // We're adding a long wait time to ensure we can stop, 1 seconds should be okay
        properties.get("writer.sleep.time").set(Integer.toString(TimeoutUtil.adjust(1000)));
        final ModelNode result = executeOperation(op);
        final long executionId = result.asLong();
        Assert.assertTrue("Execution id should be greater than 0", executionId > 0L);

        // Test the stop operation
        op = Operations.createOperation("stop-job", address);
        op.get("execution-id").set(executionId);
        executeOperation(op);

        // Validate that the job as executed
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final JobExecution execution = jobOperator.getJobExecution(executionId);
        Assert.assertNotNull(execution);

        // Wait for 1 seconds max for the execution to finish.
        waitForTermination(execution, 3);

        // Reset the counter as we're not sure how many were actually written
        currentCount = countingItemWriter.getWrittenItemSize();

        // Check that the status is stopped
        Assert.assertEquals(BatchStatus.STOPPED, execution.getBatchStatus());
    }

    @Test
    public void testStopOnExecutionResource() throws Exception {
        final ModelNode address = Operations.createAddress("deployment", DEPLOYMENT_NAME, "subsystem", "batch-jberet");
        ModelNode op = Operations.createOperation("start-job", address);
        op.get("job-xml-name").set("test-chunk");
        final ModelNode properties = op.get("properties");
        properties.get("reader.end").set("20");
        // We're adding a long wait time to ensure we can stop, 1 seconds should be okay
        properties.get("writer.sleep.time").set(Integer.toString(TimeoutUtil.adjust(1000)));
        final ModelNode result = executeOperation(op);
        final long executionId = result.asLong();
        Assert.assertTrue("Execution id should be greater than 0", executionId > 0L);

        // Test the stop operation
        final ModelNode executionAddress = Operations.createAddress("deployment", DEPLOYMENT_NAME, "subsystem", "batch-jberet", "job", "test-chunk", "execution", Long.toString(executionId));
        executeOperation(Operations.createOperation("stop-job", executionAddress));

        // Validate that the job as executed
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final JobExecution execution = jobOperator.getJobExecution(executionId);
        Assert.assertNotNull(execution);

        // Wait for 1 seconds max for the execution to finish.
        waitForTermination(execution, 3);

        // Reset the counter as we're not sure how many were actually written
        currentCount = countingItemWriter.getWrittenItemSize();

        // Check that the status is stopped
        Assert.assertEquals(BatchStatus.STOPPED, execution.getBatchStatus());
    }

    @Test
    public void testRestart() throws Exception {
        final ModelNode address = Operations.createAddress("deployment", DEPLOYMENT_NAME, "subsystem", "batch-jberet");
        ModelNode op = Operations.createOperation("start-job", address);
        op.get("job-xml-name").set("test-chunk");
        ModelNode properties = op.get("properties");
        properties.get("reader.end").set("20");
        // We're adding a long wait time to ensure we can stop, 1 seconds should be okay
        properties.get("writer.sleep.time").set(Integer.toString(TimeoutUtil.adjust(2000)));
        ModelNode result = executeOperation(op);
        long executionId = result.asLong();
        Assert.assertTrue("Execution id should be greater than 0", executionId > 0L);

        // Test the stop operation
        op = Operations.createOperation("stop-job", address);
        op.get("execution-id").set(executionId);
        executeOperation(op);

        // Validate that the job as executed
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        JobExecution execution = jobOperator.getJobExecution(executionId);
        Assert.assertNotNull(execution);

        // Wait for 5 seconds max for the execution to finish.
        waitForTermination(execution, 5);

        // Reset the counter as we're not sure how many were actually written
        currentCount = countingItemWriter.getWrittenItemSize();

        // Check that the status is stopped
        Assert.assertEquals(BatchStatus.STOPPED, execution.getBatchStatus());

        // Restart the execution
        op = Operations.createOperation("restart-job", address);
        op.get("execution-id").set(executionId);
        properties = op.get("properties");
        properties.get("reader.end").set("10");
        properties.get("writer.sleep.time").set("0");
        result = executeOperation(op);
        executionId = result.asLong();
        Assert.assertTrue("Execution id should be greater than 0", executionId > 0L);
        execution = jobOperator.getJobExecution(executionId);
        Assert.assertNotNull(execution);

        // Wait for 5 seconds max for the execution to finish.
        waitForTermination(execution, 5);

        // Check that the status is stopped
        Assert.assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
    }

    @Test
    public void testRestartOnExecutionResource() throws Exception {
        final ModelNode address = Operations.createAddress("deployment", DEPLOYMENT_NAME, "subsystem", "batch-jberet");
        ModelNode op = Operations.createOperation("start-job", address);
        op.get("job-xml-name").set("test-chunk");
        ModelNode properties = op.get("properties");
        properties.get("reader.end").set("20");
        // We're adding a long wait time to ensure we can stop, 1 seconds should be okay
        properties.get("writer.sleep.time").set(Integer.toString(TimeoutUtil.adjust(1000)));
        ModelNode result = executeOperation(op);
        long executionId = result.asLong();
        Assert.assertTrue("Execution id should be greater than 0", executionId > 0L);

        // Test the stop operation
        final ModelNode executionAddress = Operations.createAddress("deployment", DEPLOYMENT_NAME, "subsystem", "batch-jberet", "job", "test-chunk", "execution", Long.toString(executionId));
        executeOperation(Operations.createOperation("stop-job", executionAddress));

        // Validate that the job as executed
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        JobExecution execution = jobOperator.getJobExecution(executionId);
        Assert.assertNotNull(execution);

        // Wait for 1 seconds max for the execution to finish.
        waitForTermination(execution, 3);

        // Reset the counter as we're not sure how many were actually written
        currentCount = countingItemWriter.getWrittenItemSize();

        // Check that the status is stopped
        Assert.assertEquals(BatchStatus.STOPPED, execution.getBatchStatus());

        // Restart the execution
        op = Operations.createOperation("restart-job", executionAddress);
        properties = op.get("properties");
        properties.get("reader.end").set("10");
        properties.get("writer.sleep.time").set("0");
        result = executeOperation(op);
        executionId = result.asLong();
        Assert.assertTrue("Execution id should be greater than 0", executionId > 0L);
        execution = jobOperator.getJobExecution(executionId);
        Assert.assertNotNull(execution);

        // Wait for 3 seconds max for the execution to finish.
        waitForTermination(execution, 3);

        // Check that the status is stopped
        Assert.assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
    }

    private ModelNode executeOperation(final ModelNode op) throws IOException {
        final ModelControllerClient client = managementClient.getControllerClient();
        final ModelNode result = client.execute(op);
        if (Operations.isSuccessfulOutcome(result)) {
            return Operations.readResult(result);
        }
        Assert.fail(Operations.getFailureDescription(result).asString());
        // Should never be reached
        return new ModelNode();
    }


    static class DebugLoggingSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            // Enable debug logging for org.wildfly.extension.batch
            final ModelNode address = Operations.createAddress("subsystem", "logging", "logger", "org.wildfly.extension.batch");
            final ModelNode op = Operations.createAddOperation(address);
            op.get("level").set("DEBUG");
            execute(managementClient.getControllerClient(), op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            execute(managementClient.getControllerClient(), Operations.createRemoveOperation(Operations.createAddress("subsystem", "logging", "logger", "org.wildfly.extension.batch")));
        }

        static ModelNode execute(final ModelControllerClient client, final ModelNode op) throws IOException {
            final ModelNode result = client.execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                Assert.fail(Operations.getFailureDescription(result).toString());
            }
            return result;
        }
    }
}
