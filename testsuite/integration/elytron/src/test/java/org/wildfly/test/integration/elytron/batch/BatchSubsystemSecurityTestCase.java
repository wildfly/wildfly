/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.batch;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;

import java.security.AllPermission;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobSecurityException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.batch.jberet.deployment.BatchPermission;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.EJBApplicationSecurityDomainMapping;
import org.wildfly.test.security.common.elytron.PermissionRef;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.SimplePermissionMapper;

/**
 * This is for testing the BatchPermission from batch-jberet subsystem.
 * It also checks that when running a Batch job as a particular user, the security identity can be retrieved
 * within the job's code.
 *
 * The security setup is like this:
 *      user1/password1 -> can do everything
 *      user2/password2 -> can stop jobs only
 *      user3/password3 -> can read jobs only
 *
 * @author Jan Martiska
 */
@ServerSetup({BatchSubsystemSecurityTestCase.CreateBatchSecurityDomainSetupTask.class,
        BatchSubsystemSecurityTestCase.ActivateBatchSecurityDomainSetupTask.class})
@RunWith(Arquillian.class)
public class BatchSubsystemSecurityTestCase {

    static final String BATCH_SECURITY_DOMAIN_NAME = "BatchDomain";

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "batch-test.jar");
        jar.addClasses(AbstractElytronSetupTask.class,
                SecurityDomainSettingEJB.class,
                TimeoutUtil.class,
                IdentityBatchlet.class,
                FailingBatchlet.class,
                LongRunningBatchlet.class);
        jar.addAsManifestResource(BatchSubsystemSecurityTestCase.class.getPackage(),
                "assert-identity.xml",
                "batch-jobs/assert-identity.xml");
        jar.addAsManifestResource(BatchSubsystemSecurityTestCase.class.getPackage(),
                "failing-batchlet.xml",
                "batch-jobs/failing-batchlet.xml");
        jar.addAsManifestResource(BatchSubsystemSecurityTestCase.class.getPackage(),
                "long-running-batchlet.xml",
                "batch-jobs/long-running-batchlet.xml");
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new AllPermission()), "permissions.xml");
        return jar;
    }

    // this is the identityWithinJob using which the batch job was invoked
    // the job itself completes this future when it runs
    static volatile CompletableFuture<String> identityWithinJob;

    private JobOperator operator;

    @Before
    public void before() {
        operator = BatchRuntime.getJobOperator();
    }


    /**
     * Try running a job as a user who has the permission to run jobs. It should succeed.
     * The job should also be able to retrieve the name of the user who ran it.
     */
    @Test
    public void testStart_Allowed() throws Exception {
        identityWithinJob = new CompletableFuture<>();
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        user1.runAs((Callable<Long>)() -> operator.start("assert-identity", new Properties()));
        final String actualUsername = identityWithinJob.get(TimeoutUtil.adjust(20), TimeUnit.SECONDS);
        Assert.assertEquals("user1", actualUsername);
    }

    /**
     * Try running a job as a user who doesn't have the permission to run jobs. It should not succeed.
     */
    @Test
    public void testStart_NotAllowed() throws Exception {
        final SecurityIdentity user2 = getSecurityIdentity("user2", "password2");
        try {
            user2.runAs((Callable<Long>)() -> operator.start("assert-identity", new Properties()));
            Assert.fail("user2 shouldn't be allowed to start batch jobs");
        } catch(JobSecurityException e) {
            // OK
        }
    }

    /**
     * Test reading execution metadata by a user who has the permission to do it.
     * User1 runs a job and then user2 tries to read its metadata.
     */
    @Test
    public void testRead_Allowed() throws Exception {
        final Properties jobParams = new Properties();
        jobParams.put("prop1", "val1");
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        final SecurityIdentity user3 = getSecurityIdentity("user3", "password3");
        final Long executionId = user1
                .runAs((Callable<Long>)() -> operator.start("assert-identity", jobParams));
        final Properties retrievedParams = user3
                .runAs((Callable<Properties>)() -> operator.getJobExecution(executionId).getJobParameters());
        Assert.assertEquals(jobParams, retrievedParams);
    }

    /**
     * Test reading execution metadata by a user who doesn't have the permission to do it.
     * User1 runs a job and then user2 tries to read its metadata.
     */
    @Test
    public void testRead_NotAllowed() throws Exception {
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        final SecurityIdentity user2 = getSecurityIdentity("user2", "password2");
        final Long executionId = user1
                .runAs((Callable<Long>)() -> operator.start("assert-identity", new Properties()));
        try {
            user2.runAs((Callable<Properties>)() -> operator.getJobExecution(executionId).getJobParameters());
            Assert.fail("user2 shouldn't be allowed to read batch job metadata");
        } catch(JobSecurityException e) {
            // OK
        }
    }

    /**
     * Test restarting failed jobs by a user who has the permission to do it.
     */
    @Test
    public void testRestart_Allowed() throws Exception {
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        Properties params = new Properties();
        params.put("should.fail", "true");
        final Long executionId = user1
                .runAs((Callable<Long>)() -> operator.start("failing-batchlet", params));
        waitForJobEnd(executionId, 10);
        Assert.assertEquals(BatchStatus.FAILED, operator.getJobExecution(executionId).getBatchStatus());
        params.put("should.fail", "false");
        final Long executionIdAfterRestart = user1
                .runAs((Callable<Long>)() -> operator.restart(executionId, params));
        waitForJobEnd(executionIdAfterRestart, 10);
        Assert.assertEquals(BatchStatus.COMPLETED, operator.getJobExecution(executionIdAfterRestart).getBatchStatus());
    }

    /**
     * Test restarting failed jobs by a user who doesn't have the permission to do it.
     */
    @Test
    public void testRestart_NotAllowed() throws Exception {
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        final SecurityIdentity user2 = getSecurityIdentity("user2", "password2");
        Properties params = new Properties();
        params.put("should.fail", "true");
        final Long executionId = user1
                .runAs((Callable<Long>)() -> operator.start("failing-batchlet", params));
        waitForJobEnd(executionId, 10);
        Assert.assertEquals(BatchStatus.FAILED, operator.getJobExecution(executionId).getBatchStatus());
        try {
            user2.runAs((Callable<Long>)() -> operator.restart(executionId, params));
            Assert.fail("user2 shouldn't be allowed to restart batch jobs");
        } catch(JobSecurityException e) {
            // OK
        }
    }


    /**
     * Abandoning an execution by a user who has the permission to do it.
     */
    @Test
    public void testAbandon_Allowed() throws Exception {
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        final Long id = user1.runAs((Callable<Long>)() -> operator.start("assert-identity", new Properties()));
        waitForJobEnd(id, 10);
        user1.runAs(() -> operator.abandon(id));
        Assert.assertEquals(operator.getJobExecution(id).getBatchStatus(), BatchStatus.ABANDONED);
    }

    /**
     * Abandoning an execution by a user who doesn't have the permission to do it.
     */
    @Test
    public void testAbandon_NotAllowed() throws Exception {
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        final SecurityIdentity user2 = getSecurityIdentity("user2", "password2");
        final Long id = user1.runAs((Callable<Long>)() -> operator.start("assert-identity", new Properties()));
        waitForJobEnd(id, 10);
        try {
            user2.runAs(() -> operator.abandon(id));
            Assert.fail("user2 should not be allowed to abandon job executions");
        } catch(JobSecurityException e) {
            // OK
        }
        Assert.assertEquals(operator.getJobExecution(id).getBatchStatus(), BatchStatus.COMPLETED);
    }

    /**
     * Stopping an execution by a user who doesn't have the permission to do it.
     */
    @Test
    public void testStop_NotAllowed() throws Exception {
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        final SecurityIdentity user3 = getSecurityIdentity("user3", "password3");
        final Long id = user1.runAs((Callable<Long>)() -> operator.start("long-running-batchlet", null));
        TimeUnit.SECONDS.sleep(1);
        try {
            user3.runAs(() -> operator.stop(id));
            Assert.fail("user2 should not be allowed to stop job executions");
        } catch(JobSecurityException e) {
            // OK
        }
        Assert.assertNotEquals(BatchStatus.STOPPED, operator.getJobExecution(id).getBatchStatus());
    }

    /**
     * Stopping an execution by a user who has the permission to do it.
     */
    @Test
    public void testStop_Allowed() throws Exception {
        final SecurityIdentity user1 = getSecurityIdentity("user1", "password1");
        final Long id = user1.runAs((Callable<Long>)() -> operator.start("long-running-batchlet", null));
        TimeUnit.SECONDS.sleep(1);
        user1.runAs(() -> operator.stop(id));
        waitForJobEnd(id, 10);
        Assert.assertEquals(BatchStatus.STOPPED, operator.getJobExecution(id).getBatchStatus());
    }


    private void waitForJobEnd(Long id, int timeoutSeconds) throws TimeoutException {
        Long start = System.currentTimeMillis();
        final JobOperator operator = BatchRuntime.getJobOperator();
        while(System.currentTimeMillis() - start < (TimeoutUtil.adjust(timeoutSeconds) * 1000)) {
            if(operator.getJobExecution(id).getEndTime() != null)
                return;
        }
        throw new TimeoutException();
    }

    private static SecurityIdentity getSecurityIdentity(String username, String password)
            throws RealmUnavailableException {
        return SecurityDomain.getCurrent().authenticate(username, new PasswordGuessEvidence(password.toCharArray()));
    }

    static class CreateBatchSecurityDomainSetupTask extends AbstractElytronSetupTask {
        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[] {
                    SimplePermissionMapper.builder().withName("batch-permission-mapper")
                            .permissionMappings(
                                    SimplePermissionMapper.PermissionMapping.builder()
                                            .withPrincipals("user1", "anonymous")
                                            .withPermissions(
                                                    PermissionRef.builder()
                                                            .targetName("*")
                                                            .className(BatchPermission.class.getName())
                                                            .module("org.wildfly.extension.batch.jberet")
                                                            .build(),
                                                    PermissionRef.builder()
                                                            .className(LoginPermission.class.getName())
                                                            .build())
                                            .build(),
                                    SimplePermissionMapper.PermissionMapping.builder()
                                            .withPrincipals("user2")
                                            .withPermissions(
                                                    PermissionRef.builder()
                                                            .targetName("stop")
                                                            .className(BatchPermission.class.getName())
                                                            .module("org.wildfly.extension.batch.jberet")
                                                            .build(),
                                                    PermissionRef.builder()
                                                            .className(LoginPermission.class.getName())
                                                            .build())
                                            .build(),
                                    SimplePermissionMapper.PermissionMapping.builder()
                                            .withPrincipals("user3")
                                            .withPermissions(
                                                    PermissionRef.builder()
                                                            .targetName("read")
                                                            .className(BatchPermission.class.getName())
                                                            .module("org.wildfly.extension.batch.jberet")
                                                            .build(),
                                                    PermissionRef.builder()
                                                            .className(LoginPermission.class.getName())
                                                            .build())
                                            .build()
                            ).build(),
                    PropertyFileBasedDomain.builder().withName(BATCH_SECURITY_DOMAIN_NAME)
                            .permissionMapper("batch-permission-mapper")
                            .withUser("user1", "password1")
                            .withUser("user2", "password2")
                            .withUser("user3", "password3")
                            .build(),
                    new EJBApplicationSecurityDomainMapping(BATCH_SECURITY_DOMAIN_NAME, BATCH_SECURITY_DOMAIN_NAME)
            };
        }
    }

    static class ActivateBatchSecurityDomainSetupTask extends SnapshotRestoreSetupTask {

        final ModelNode BATCH_SUBSYSTEM_ADDRESS = PathAddress.pathAddress("subsystem", "batch-jberet")
                .toModelNode();

        @Override
        public void doSetup(ManagementClient managementClient, String s) throws Exception {
            final ModelNode setOp = new ModelNode();
            setOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            setOp.get(ADDRESS).set(BATCH_SUBSYSTEM_ADDRESS);
            setOp.get("name").set("security-domain");
            setOp.get("value").set(BATCH_SECURITY_DOMAIN_NAME);

            final ModelNode result = managementClient.getControllerClient().execute(setOp);
            Assert.assertTrue(result.get("outcome").asString().equals("success"));

            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }

    }
}
