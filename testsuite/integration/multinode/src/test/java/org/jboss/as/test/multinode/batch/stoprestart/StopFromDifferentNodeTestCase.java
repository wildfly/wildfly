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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.multinode.ejb.timer.database.DatabaseTimerServiceMultiNodeExecutionDisabledTestCase.getRemoteContext;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.net.SocketPermission;
import java.security.SecurityPermission;
import java.util.Collections;
import java.util.Properties;
import javax.batch.runtime.BatchStatus;
import javax.naming.Context;

import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(StopFromDifferentNodeTestCase.StopFromDifferentNodeTestCaseSetup.class)
public class StopFromDifferentNodeTestCase {
    private static final String ARCHIVE_NAME = "stopFromDifferentNode";
    private static final String BATCHLET1_JOB = "batchlet1.xml";
    private static final String BATCH_CLIENT_BEAN_LOOKUP = ARCHIVE_NAME + "/" + BatchClientBean.class.getSimpleName() + "!" + BatchClientIF.class.getName();
    private static final long BATCHLET_DELAY_SECONDS = TimeoutUtil.adjust(2);

    private static Server h2Server;

    @AfterClass
    public static void afterClass() {
        if (h2Server != null) {
            h2Server.stop();
        }
    }

    static class StopFromDifferentNodeTestCaseSetup implements ServerSetupTask {
        static final PathAddress ADDR_DATA_SOURCE = PathAddress.pathAddress().append(SUBSYSTEM, "datasources").append("data-source", "MyNewDs");
        static final PathAddress ADDR_BATCH_SUBSYSTEM = PathAddress.pathAddress().append(SUBSYSTEM, "batch-jberet");
        static final PathAddress ADDR_JDBC_JOB_REPOSITORY = ADDR_BATCH_SUBSYSTEM.append("jdbc-job-repository", "jdbc");

        String savedDefaultJobRepository = null;

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            if (h2Server == null) {
                //we need a TCP server that can be shared between the two servers
                h2Server = Server.createTcpServer().start();
            }

            if (savedDefaultJobRepository == null) {
                final ModelNode readAttributeOperation = Util.getReadAttributeOperation(ADDR_BATCH_SUBSYSTEM, "default-job-repository");
                final ModelNode defaultJobRepository = managementClient.getControllerClient().execute(readAttributeOperation);
                savedDefaultJobRepository = defaultJobRepository.get("result").asString("in-memory");
            }

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /subsystem=datasources/data-source=MyNewDs:add(name=MyNewDs,jndi-name=java:jboss/datasources/MyNewDs, enabled=true)
            ModelNode datasourceAddModelNode = Util.createAddOperation(ADDR_DATA_SOURCE);
            datasourceAddModelNode.get("name").set("MyNewDs");
            datasourceAddModelNode.get("jndi-name").set("java:jboss/datasources/MyNewDs");
            datasourceAddModelNode.get("enabled").set(true);
            datasourceAddModelNode.get("driver-name").set("h2");
            datasourceAddModelNode.get("pool-name").set("MyNewDs_Pool");
            datasourceAddModelNode.get("connection-url").set("jdbc:h2:" + h2Server.getURL() + "/mem:testdb;DB_CLOSE_DELAY=-1");
            datasourceAddModelNode.get("user-name").set("sa");
            datasourceAddModelNode.get("password").set("sa");
            steps.add(datasourceAddModelNode);

            // /subsystem=batch-jberet/jdbc-job-repository=jdbc:add(data-source=MyNewDs)
            ModelNode jdbcJobRepositoryAddModelNode = Util.createAddOperation(ADDR_JDBC_JOB_REPOSITORY);
            jdbcJobRepositoryAddModelNode.get("data-source").set("MyNewDs");
            steps.add(jdbcJobRepositoryAddModelNode);

            ModelNode setJobRepositoryModelNode = Util.getWriteAttributeOperation(ADDR_BATCH_SUBSYSTEM, "default-job-repository", "jdbc");
            steps.add(setJobRepositoryModelNode);

            Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            ModelNode setJobRepositoryModelNode = Util.getWriteAttributeOperation(ADDR_BATCH_SUBSYSTEM, "default-job-repository", savedDefaultJobRepository);
            steps.add(setJobRepositoryModelNode);

            ModelNode jdbcJobRepositoryRemoveModelNode = Util.createRemoveOperation(ADDR_JDBC_JOB_REPOSITORY);
            steps.add(jdbcJobRepositoryRemoveModelNode);

            ModelNode datasourceRemoveModelNode = Util.createRemoveOperation(ADDR_DATA_SOURCE);
            steps.add(datasourceRemoveModelNode);

            Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }
    }

    @ContainerResource("multinode-server")
    private ManagementClient client1;

    @ContainerResource("multinode-client")
    private ManagementClient client2;

    @Deployment(name = "server", testable = false)
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment() {
        return createDeployment();
    }

    @Deployment(name = "client", testable = false)
    @TargetsContainer("multinode-client")
    public static Archive<?> clientDeployment() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(Batchlet1.class, BatchClientIF.class, BatchClientBean.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsWebInfResource(StopFromDifferentNodeTestCase.class.getPackage(), BATCHLET1_JOB, "classes/META-INF/batch-jobs/" + BATCHLET1_JOB);
        war.addAsManifestResource(
                createPermissionsXmlAsset(
                        new SocketPermission("*:9092", "connect,resolve"),
                        new SecurityPermission("putProviderProperty.WildFlyElytron")),
                "permissions.xml");
        return war;
    }

    /**
     * Verifies that a running batch job execution can be stopped from a different node.
     * This test starts a batch job in node 1, stop it in node 2, and restart it in node 2.
     */
    @Test
    public void testStartStopRestart122() throws Exception {
        testStartStopRestart(122);
    }

    /**
     * Verifies that a running batch job execution can be stopped from a different node.
     * This test starts a batch job in node 1, stop it in node 2, and restart it in node 1.
     */
    @Test
    public void testStartStopRestart121() throws Exception {
        testStartStopRestart(121);
    }

    /**
     * Starts a job execution, stops it from a different node, and then restart it.
     *
     * @param sequence if 121, the restart will be from the same node where the job was initially started;
     *                 if 122, the restart will be from the same node where the job was stopped.
     * @throws Exception on any exception
     */
    private void testStartStopRestart(int sequence) throws Exception {
        Context context1 = null;
        Context context2 = null;

        try {
            context1 = getRemoteContext(client1);
            context2 = getRemoteContext(client2);
            BatchClientIF bean1 = (BatchClientIF) context1.lookup(BATCH_CLIENT_BEAN_LOOKUP);
            BatchClientIF bean2 = (BatchClientIF) context2.lookup(BATCH_CLIENT_BEAN_LOOKUP);

            //start the job in node 1
            final Properties jobParams = new Properties();
            jobParams.setProperty("seconds", String.valueOf(BATCHLET_DELAY_SECONDS));
            final long jobExecutionId = bean1.start(BATCHLET1_JOB, jobParams);

            //make sure the job execution has started
            final BatchStatus startedFromNode1 = waitForBatchStatus(bean1, jobExecutionId, BatchStatus.STARTED);
            assertEquals(BatchStatus.STARTED, startedFromNode1);
            final BatchStatus startedFromNode2 = waitForBatchStatus(bean2, jobExecutionId, BatchStatus.STARTED);
            assertEquals(BatchStatus.STARTED, startedFromNode2);

            //stop the job execution from node 2
            bean2.stop(jobExecutionId);

            //check job status from node 1
            final BatchStatus stoppedFromNode1 = waitForBatchStatus(bean1, jobExecutionId, BatchStatus.STOPPED);
            assertEquals(BatchStatus.STOPPED, stoppedFromNode1);

            //check job status from node 2
            final BatchStatus stoppedFromNode2 = waitForBatchStatus(bean2, jobExecutionId, BatchStatus.STOPPED);
            assertEquals(BatchStatus.STOPPED, stoppedFromNode2);

            //restart from node 1 or node 2, depending on sequence parameter.
            BatchClientIF beanUsedToRestart = sequence == 121 ? bean1 : bean2;
            final long restartExecutionId = beanUsedToRestart.restart(jobExecutionId, null);

            //check job status from node 1
            final BatchStatus restartStatusFromNode1 = waitForBatchStatus(bean1, restartExecutionId, BatchStatus.COMPLETED);
            assertEquals(BatchStatus.COMPLETED, restartStatusFromNode1);

            //check job status from node 2
            final BatchStatus restartStatusFromNode2 = waitForBatchStatus(bean2, restartExecutionId, BatchStatus.COMPLETED);
            assertEquals(BatchStatus.COMPLETED, restartStatusFromNode2);
        } finally {
            if (context2 != null) {
                context2.close();
            }
            if (context1 != null) {
                context1.close();
            }
        }
    }

    private static BatchStatus waitForBatchStatus(BatchClientIF bean, long jobExecutionId, BatchStatus batchStatus) throws Exception {
        final int checkIntervalMillis = 50;
        final int maxCount = 50;
        int count = 0;
        BatchStatus finalStatus = null;
        do {
            Thread.sleep(checkIntervalMillis);
            try {
                finalStatus = bean.getJobStatus(jobExecutionId);
            } catch (Exception e) {
                //ignore
            }
        } while (finalStatus != batchStatus && ++count <= maxCount);

        return finalStatus;
    }

}
