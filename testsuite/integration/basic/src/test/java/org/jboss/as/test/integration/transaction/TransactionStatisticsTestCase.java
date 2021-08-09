/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.transaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.io.FilePermission;
import java.util.PropertyPermission;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_GROUP_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

@RunWith(Arquillian.class)
@ServerSetup({TransactionStatisticsTestCase.TransactionEnabledSetup.class})
public class TransactionStatisticsTestCase {
    private static final PathAddress TX_SUBSYSTEM_ADDRESS = PathAddress.pathAddress().append(SUBSYSTEM, "transactions");
    // for available attributes see org.jboss.as.txn.subsystem.TxStatsHandler
    private static final String STATISTICS_ENABLED_ATTR = "statistics-enabled";
    private static final String GROUP_STATISTICS_ATTR = "statistics";
    private static final String NUMBER_OF_TRANSACTIONS_ATTR = "number-of-transactions";
    private static final String NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR = "number-of-committed-transactions";
    private static final String NUMBER_OF_ABORTED_TRANSACTIONS_ATTR = "number-of-aborted-transactions";
    private static final String NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR = "number-of-application-rollbacks";
    private static final String NUMBER_OF_NESTED_TRANSACTIONS_ATTR = "number-of-nested-transactions";
    private static final String NUMBER_OF_HEURISTICS_ATTR = "number-of-heuristics";
    private static final String NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR = "number-of-inflight-transactions";
    private static final String NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR = "number-of-timed-out-transactions";
    private static final String NUMBER_OF_RESOURCE_ROLLBACKS_ATTR = "number-of-resource-rollbacks";
    private static final String NUMBER_OF_SYSTEM_ROLLBACKS_ATTR = "number-of-system-rollbacks";
    private static final String AVERAGE_COMMIT_TIME_ATTR = "average-commit-time";

    int numberBefore, numberCommittedBefore, numberAbortedBefore, numberAppAbortedBefore, numberNestedBefore,
        numberHeuristicsBefore, numberInflightBefore, numberTimedOutBefore, numberResourceRollbackBefore, numberSystemRollbackBefore;

    static class TransactionEnabledSetup implements ServerSetupTask {
        private ModelNode statisticsEnabledOriginValue;
        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode opResult = MaximumTimeoutTestCase.executeForResult(
                    managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, STATISTICS_ENABLED_ATTR));
            // value can be either expression or boolean, let's tackle it with try/catch
            try {
                ValueExpression originalAsExpression = opResult.asExpression();
                statisticsEnabledOriginValue = new ModelNode().set(originalAsExpression);
            } catch (Exception notAnExpression) {
                // let's try to use boolean
                boolean originalAsBoolean = opResult.asBoolean();
                statisticsEnabledOriginValue = new ModelNode().set(originalAsBoolean);
            }
            writeStatisticsEnabled(managementClient, new ModelNode().set(true));
        }
        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            writeStatisticsEnabled(managementClient, statisticsEnabledOriginValue);
        }
        private void writeStatisticsEnabled(ManagementClient client, ModelNode modelNodeWriteData) {
            ModelNode op = Util.getWriteAttributeOperation(TX_SUBSYSTEM_ADDRESS, STATISTICS_ENABLED_ATTR, modelNodeWriteData);
            MaximumTimeoutTestCase.executeForResult(client.getControllerClient(), op);
        }
    }

    @Resource(mappedName = "java:/TransactionManager")
    private static TransactionManager tm;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(TransactionStatisticsTestCase.class, TransactionStatisticsTestCase.TransactionEnabledSetup.class,
                        MaximumTimeoutTestCase.class, TimeoutUtil.class)
                .addPackage(TestXAResource.class.getPackage())
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, org.jboss.remoting\n"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                    // ManagementClient needs the following permissions and a dependency on 'org.jboss.remoting3' module
                    new RemotingPermission("createEndpoint"),
                    new RemotingPermission("connect"),
                    new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read"),
                    // TimeoutUtil.adjust needs permission for system property reading
                    new PropertyPermission("ts.timeout.factor", "read")
                ), "permissions.xml");
    }

    @Before
    public void readStatisticsBeforeTest() {
        numberBefore = readInt(NUMBER_OF_TRANSACTIONS_ATTR);
        numberCommittedBefore = readInt(NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR);
        numberAbortedBefore = readInt(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR);
        numberAppAbortedBefore = readInt(NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR);
        numberNestedBefore = readInt(NUMBER_OF_NESTED_TRANSACTIONS_ATTR);
        numberHeuristicsBefore = readInt(NUMBER_OF_HEURISTICS_ATTR);
        numberInflightBefore = readInt(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR);
        numberTimedOutBefore = readInt(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR);
        numberResourceRollbackBefore = readInt(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR);
        numberSystemRollbackBefore = readInt(NUMBER_OF_SYSTEM_ROLLBACKS_ATTR);
    }

    @Test
    public void transactionStatisticsGrouped() throws Exception {
        XAResource xaer = new TestXAResource();
        tm.begin();
        tm.getTransaction().enlistResource(xaer);
        tm.commit();

        tm.begin();
        tm.getTransaction().enlistResource(xaer);
        tm.rollback();

        Assert.assertEquals("Two more transactions in statistics are expected",
                numberBefore + 2, readInt(NUMBER_OF_TRANSACTIONS_ATTR));
        Assert.assertEquals("One more committed transaction in statistics is expected",
                numberCommittedBefore + 1, readInt(NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR));
        Assert.assertEquals("One more aborted transaction in statistics is expected",
                numberAbortedBefore + 1, readInt(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR));
        Assert.assertEquals("One more application aborted transaction in statistics is expected",
                numberAppAbortedBefore + 1, readInt(NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR));
        Assert.assertEquals("No additional nested transaction is expected",
                numberNestedBefore, readInt(NUMBER_OF_NESTED_TRANSACTIONS_ATTR));
        Assert.assertEquals("No additional heuristic transaction is expected",
                numberHeuristicsBefore, readInt(NUMBER_OF_HEURISTICS_ATTR));
        Assert.assertEquals("No additional inflight transaction is expected",
                numberInflightBefore, readInt(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR));
        Assert.assertEquals("No additional timed out transaction is expected",
                numberTimedOutBefore, readInt(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR));
        Assert.assertEquals("No additional resource rollback transaction is expected",
                numberResourceRollbackBefore, readInt(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR));
        Assert.assertEquals("No additional system rollback transaction is expected",
                numberSystemRollbackBefore, readInt(NUMBER_OF_SYSTEM_ROLLBACKS_ATTR));
        Assert.assertTrue("Expected the commit time was non zero value", readAverage() > 0L);

        ModelNode grouped = readGrouped();
        Assert.assertEquals("Two more transactions listed in grouped statistics are expected",
                numberBefore + 2, grouped.get(NUMBER_OF_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("One more committed transaction listed in grouped statistics is expected",
                numberCommittedBefore + 1, grouped.get(NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("One more aborted transaction listed in grouped statistics is expected",
                numberAbortedBefore + 1, grouped.get(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("One more application aborted transaction listed in grouped statistics is expected",
                numberAppAbortedBefore + 1, grouped.get(NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("No additional nested transaction is expected in grouped statistics",
                numberNestedBefore, grouped.get(NUMBER_OF_NESTED_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("No additional heuristic transaction is expected in grouped statistics",
                numberHeuristicsBefore, grouped.get(NUMBER_OF_HEURISTICS_ATTR).asInt());
        Assert.assertEquals("No additional inflight transaction is expected in grouped statistics",
                numberInflightBefore, grouped.get(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("No additional timed out transaction is expected in grouped statistics",
                numberTimedOutBefore, grouped.get(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("No additional resource rollback transaction is expected in grouped statistics",
                numberResourceRollbackBefore, grouped.get(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR).asInt());
        Assert.assertEquals("No additional system rollback transaction is expected in grouped statistics",
                numberSystemRollbackBefore, grouped.get(NUMBER_OF_SYSTEM_ROLLBACKS_ATTR).asInt());
        Assert.assertTrue("Expected the commit time was non zero value and is listed in grouped statistics",
                grouped.get(AVERAGE_COMMIT_TIME_ATTR).asLong() > 0L);
    }

    @Test
    public void transactionStatisticsInflight() throws Exception {
        tm.begin();
        Assert.assertEquals("One more inflight transaction is expected being in progress",
                numberInflightBefore + 1, readInt(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR));
        ModelNode grouped = readGrouped();
        Assert.assertEquals("One more inflight transaction is expected in grouped statistics",
                numberInflightBefore + 1, grouped.get(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR).asInt());
        tm.commit();
    }

    @Test
    public void transactionStatisticsTimeout() throws Exception {
        try {
            tm.setTransactionTimeout(1);
            tm.begin();
            Thread.sleep(TimeoutUtil.adjust(1100));
            tm.commit();
            Assert.fail("Expected the transaction commit fails as transaction was rolled-back with timeout");
        } catch (RollbackException expected) {
        } finally {
            tm.setTransactionTimeout(0);
        }
        Assert.assertEquals("One more timed out transaction is expected",
                numberTimedOutBefore + 1, readInt(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR));
        ModelNode grouped = readGrouped();
        Assert.assertEquals("One more timed out transaction is expected in grouped statistics",
                numberTimedOutBefore + 1, grouped.get(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR).asInt());
    }

    @Test
    public void transactionStatisticsHeuristics() throws Exception {
        XAResource xaer = new TestXAResource();
        XAResource xaerHeur = new TestXAResource(TestXAResource.TestAction.COMMIT_THROW_XAER_RMERR);
        try {
            tm.begin();
            tm.getTransaction().enlistResource(xaer);
            tm.getTransaction().enlistResource(xaerHeur);
            tm.commit();
            Assert.fail("Expected the transaction commit fails as transaction was marked as heuristics by XAException");
        } catch (HeuristicMixedException expected) {
        }

        Assert.assertEquals("One more heuristic transaction is expected",
                numberHeuristicsBefore + 1, readInt(NUMBER_OF_HEURISTICS_ATTR));
        ModelNode grouped = readGrouped();
        Assert.assertEquals("One more heuristic transaction is expected in grouped statistics",
                numberHeuristicsBefore + 1, grouped.get(NUMBER_OF_HEURISTICS_ATTR).asInt());
    }

    @Test
    public void transactionStatisticsResourceRollback() throws Exception {
        XAResource xaerHeur = new TestXAResource(TestXAResource.TestAction.COMMIT_THROW_XA_RBROLLBACK);
        XAResource xaer = new TestXAResource();
        try {
            tm.begin();
            tm.getTransaction().enlistResource(xaerHeur);
            tm.getTransaction().enlistResource(xaer);
            tm.commit();
            Assert.fail("Expected the transaction commit fails as transaction commit failed with XAException");
        } catch (RollbackException expected) {
        }

        Assert.assertEquals("One more resource rollback transaction is expected",
                numberResourceRollbackBefore + 1, readInt(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR));
        Assert.assertEquals("One more aborted transaction is expected",
                numberAbortedBefore + 1, readInt(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR));
        ModelNode grouped = readGrouped();
        Assert.assertEquals("One more resource rollback transaction is expected in grouped statistics",
                numberResourceRollbackBefore + 1, grouped.get(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR).asInt());
        Assert.assertEquals("One more aborted transaction is expected in grouped statistics",
                numberAbortedBefore + 1, grouped.get(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR).asInt());
    }

    private int readInt(String attributeName) {
        ModelNode opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, attributeName));
        return opResult.asInt();
    }

    private long readAverage() {
        ModelNode opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, AVERAGE_COMMIT_TIME_ATTR));
        return opResult.asLong();
    }

    private ModelNode readGrouped() {
        ModelNode groupReadOp = Util.createEmptyOperation(READ_ATTRIBUTE_GROUP_OPERATION, TX_SUBSYSTEM_ADDRESS);
        groupReadOp.get(NAME).set(GROUP_STATISTICS_ATTR);
        groupReadOp.get(INCLUDE_RUNTIME).set(true);
        return MaximumTimeoutTestCase.executeForResult(managementClient.getControllerClient(), groupReadOp).asObject();
    }
}
