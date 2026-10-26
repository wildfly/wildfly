/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.annotation.Resource;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.io.FilePermission;
import java.util.PropertyPermission;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_GROUP_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

@ExtendWith(ArquillianExtension.class)
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

    @BeforeEach
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

        Assertions.assertEquals(numberBefore + 2,
                readInt(NUMBER_OF_TRANSACTIONS_ATTR), "Two more transactions in statistics are expected");
        Assertions.assertEquals(numberCommittedBefore + 1,
                readInt(NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR), "One more committed transaction in statistics is expected");
        Assertions.assertEquals(numberAbortedBefore + 1,
                readInt(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR), "One more aborted transaction in statistics is expected");
        Assertions.assertEquals(numberAppAbortedBefore + 1,
                readInt(NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR), "One more application aborted transaction in statistics is expected");
        Assertions.assertEquals(numberNestedBefore,
                readInt(NUMBER_OF_NESTED_TRANSACTIONS_ATTR), "No additional nested transaction is expected");
        Assertions.assertEquals(numberHeuristicsBefore,
                readInt(NUMBER_OF_HEURISTICS_ATTR), "No additional heuristic transaction is expected");
        Assertions.assertEquals(numberInflightBefore,
                readInt(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR), "No additional inflight transaction is expected");
        Assertions.assertEquals(numberTimedOutBefore,
                readInt(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR), "No additional timed out transaction is expected");
        Assertions.assertEquals(numberResourceRollbackBefore,
                readInt(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR), "No additional resource rollback transaction is expected");
        Assertions.assertEquals(numberSystemRollbackBefore,
                readInt(NUMBER_OF_SYSTEM_ROLLBACKS_ATTR), "No additional system rollback transaction is expected");
        Assertions.assertTrue(readAverage() > 0L, "Expected the commit time was non zero value");

        ModelNode grouped = readGrouped();
        Assertions.assertEquals(numberBefore + 2,
                grouped.get(NUMBER_OF_TRANSACTIONS_ATTR).asInt(), "Two more transactions listed in grouped statistics are expected");
        Assertions.assertEquals(numberCommittedBefore + 1,
                grouped.get(NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR).asInt(), "One more committed transaction listed in grouped statistics is expected");
        Assertions.assertEquals(numberAbortedBefore + 1,
                grouped.get(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR).asInt(), "One more aborted transaction listed in grouped statistics is expected");
        Assertions.assertEquals(numberAppAbortedBefore + 1,
                grouped.get(NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR).asInt(), "One more application aborted transaction listed in grouped statistics is expected");
        Assertions.assertEquals(numberNestedBefore,
                grouped.get(NUMBER_OF_NESTED_TRANSACTIONS_ATTR).asInt(), "No additional nested transaction is expected in grouped statistics");
        Assertions.assertEquals(numberHeuristicsBefore,
                grouped.get(NUMBER_OF_HEURISTICS_ATTR).asInt(), "No additional heuristic transaction is expected in grouped statistics");
        Assertions.assertEquals(numberInflightBefore,
                grouped.get(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR).asInt(), "No additional inflight transaction is expected in grouped statistics");
        Assertions.assertEquals(numberTimedOutBefore,
                grouped.get(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR).asInt(), "No additional timed out transaction is expected in grouped statistics");
        Assertions.assertEquals(numberResourceRollbackBefore,
                grouped.get(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR).asInt(), "No additional resource rollback transaction is expected in grouped statistics");
        Assertions.assertEquals(numberSystemRollbackBefore,
                grouped.get(NUMBER_OF_SYSTEM_ROLLBACKS_ATTR).asInt(), "No additional system rollback transaction is expected in grouped statistics");
        Assertions.assertTrue(grouped.get(AVERAGE_COMMIT_TIME_ATTR).asLong() > 0L,
                "Expected the commit time was non zero value and is listed in grouped statistics");
    }

    @Test
    public void transactionStatisticsInflight() throws Exception {
        tm.begin();
        Assertions.assertEquals(numberInflightBefore + 1,
                readInt(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR), "One more inflight transaction is expected being in progress");
        ModelNode grouped = readGrouped();
        Assertions.assertEquals(numberInflightBefore + 1,
                grouped.get(NUMBER_OF_INFLIGHT_TRANSACTIONS_ATTR).asInt(), "One more inflight transaction is expected in grouped statistics");
        tm.commit();
    }

    @Test
    public void transactionStatisticsTimeout() throws Exception {
        try {
            tm.setTransactionTimeout(1);
            tm.begin();
            Thread.sleep(TimeoutUtil.adjust(1100));
            tm.commit();
            Assertions.fail("Expected the transaction commit fails as transaction was rolled-back with timeout");
        } catch (RollbackException expected) {
        } finally {
            tm.setTransactionTimeout(0);
        }
        Assertions.assertEquals(numberTimedOutBefore + 1,
                readInt(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR), "One more timed out transaction is expected");
        ModelNode grouped = readGrouped();
        Assertions.assertEquals(numberTimedOutBefore + 1,
                grouped.get(NUMBER_OF_TIMED_OUT_TRANSACTIONS_ATTR).asInt(), "One more timed out transaction is expected in grouped statistics");
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
            Assertions.fail("Expected the transaction commit fails as transaction was marked as heuristics by XAException");
        } catch (HeuristicMixedException expected) {
        }

        Assertions.assertEquals(numberHeuristicsBefore + 1,
                readInt(NUMBER_OF_HEURISTICS_ATTR), "One more heuristic transaction is expected");
        ModelNode grouped = readGrouped();
        Assertions.assertEquals(numberHeuristicsBefore + 1,
                grouped.get(NUMBER_OF_HEURISTICS_ATTR).asInt(), "One more heuristic transaction is expected in grouped statistics");
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
            Assertions.fail("Expected the transaction commit fails as transaction commit failed with XAException");
        } catch (RollbackException expected) {
        }

        Assertions.assertEquals(numberResourceRollbackBefore + 1,
                readInt(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR), "One more resource rollback transaction is expected");
        Assertions.assertEquals(numberAbortedBefore + 1,
                readInt(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR), "One more aborted transaction is expected");
        ModelNode grouped = readGrouped();
        Assertions.assertEquals(numberResourceRollbackBefore + 1,
                grouped.get(NUMBER_OF_RESOURCE_ROLLBACKS_ATTR).asInt(), "One more resource rollback transaction is expected in grouped statistics");
        Assertions.assertEquals(numberAbortedBefore + 1,
                grouped.get(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR).asInt(), "One more aborted transaction is expected in grouped statistics");
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
