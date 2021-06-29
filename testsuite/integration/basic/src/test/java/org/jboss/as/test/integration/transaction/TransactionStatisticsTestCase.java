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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.io.FilePermission;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_GROUP_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

@RunWith(Arquillian.class)
@ServerSetup({TransactionStatisticsTestCase.TransactionEnabledSetup.class})
public class TransactionStatisticsTestCase {
    private static final PathAddress TX_SUBSYSTEM_ADDRESS = PathAddress.pathAddress().append(SUBSYSTEM, "transactions");
    private static final String STATISTICS_ENABLED_ATTR = "statistics-enabled";
    private static final String NUMBER_OF_TRANSACTIONS_ATTR = "number-of-transactions";
    private static final String NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR = "number-of-committed-transactions";
    private static final String NUMBER_OF_ABORTED_TRANSACTIONS_ATTR = "number-of-aborted-transactions";
    private static final String NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR = "number-of-application-rollbacks";
    private static final String GROUP_STATISTICS_ATTR = "statistics";

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
                        MaximumTimeoutTestCase.class)
                .addPackage(TestXAResource.class.getPackage())
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, org.jboss.remoting\n"), "MANIFEST.MF")
                .addAsManifestResource(createPermissionsXmlAsset(
                    // ManagementClient needs the following permissions and a dependency on 'org.jboss.remoting3' module
                    new RemotingPermission("createEndpoint"),
                    new RemotingPermission("connect"),
                    new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
                ), "permissions.xml");
    }

    @Test
    public void transactionStatisticsGrouped() throws Exception{
        ModelNode opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_TRANSACTIONS_ATTR));
        int numberBefore = opResult.asInt();
        opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR));
        int numberCommittedBefore = opResult.asInt();
        opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_ABORTED_TRANSACTIONS_ATTR));
        int numberAbortedBefore = opResult.asInt();
        opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR));
        int numberAppAbortedBefore = opResult.asInt();

        XAResource xaer = new TestXAResource();
        tm.begin();
        tm.getTransaction().enlistResource(xaer);
        tm.commit();

        tm.begin();
        tm.getTransaction().enlistResource(xaer);
        tm.rollback();

        opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_TRANSACTIONS_ATTR));
        Assert.assertEquals("Two more transactions in statistics are expected", numberBefore + 2, opResult.asInt());
        opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR));
        Assert.assertEquals("One more committed transaction in statistics is expected", numberCommittedBefore + 1, opResult.asInt());
        opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_ABORTED_TRANSACTIONS_ATTR));
        Assert.assertEquals("One more aborted transaction in statistics is expected", numberAbortedBefore + 1, opResult.asInt());
        opResult = MaximumTimeoutTestCase.executeForResult(
                managementClient.getControllerClient(), Util.getReadAttributeOperation(TX_SUBSYSTEM_ADDRESS, NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR));
        Assert.assertEquals("One more application aborted transaction in statistics is expected", numberAppAbortedBefore + 1, opResult.asInt());


        ModelNode groupReadOp = Util.createEmptyOperation(READ_ATTRIBUTE_GROUP_OPERATION, TX_SUBSYSTEM_ADDRESS);
        groupReadOp.get(NAME).set(GROUP_STATISTICS_ATTR);
        groupReadOp.get(INCLUDE_RUNTIME).set(true);
        opResult = MaximumTimeoutTestCase.executeForResult(managementClient.getControllerClient(), groupReadOp).asObject();
        Assert.assertEquals("Two more transactions listed in grouped statistics are expected",
                numberBefore + 2, opResult.get(NUMBER_OF_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("One more committed transaction listed in grouped statistics is expected",
                numberCommittedBefore + 1, opResult.get(NUMBER_OF_COMMITTED_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("One more aborted transaction listed in grouped statistics is expected",
                numberAbortedBefore + 1, opResult.get(NUMBER_OF_ABORTED_TRANSACTIONS_ATTR).asInt());
        Assert.assertEquals("One more application aborted transaction listed in grouped statistics is expected",
                numberAppAbortedBefore + 1, opResult.get(NUMBER_OF_APP_ABORTED_TRANSACTIONS_ATTR).asInt());
    }

}
