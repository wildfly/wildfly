/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.txn.subsystem;

import java.io.IOException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import com.arjuna.ats.arjuna.coordinator.TxStats;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TransactionSubsystemTestCase extends AbstractSubsystemBaseTest {


    public TransactionSubsystemTestCase() {
        super(TransactionExtension.SUBSYSTEM_NAME, new TransactionExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-txn_6_0.xsd";
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        String transformed = ModelTestUtils.normalizeXML(
                original.replace("enable-statistics", "statistics-enabled")
                        .replace("use-hornetq-store", "use-journal-store"));
        super.compareXml(configId, transformed, marshalled, true);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("full-expressions.xml");
    }

    @Test
    public void testMinimalConfig() throws Exception {
        standardSubsystemTest("minimal.xml");
    }

    @Test
    public void testJdbcStore() throws Exception {
        standardSubsystemTest("jdbc-store.xml");
    }

    @Test
    public void testCmr() throws Exception {
        standardSubsystemTest("cmr.xml");
    }

    @Test
    public void testJdbcStoreMinimal() throws Exception {
        standardSubsystemTest("jdbc-store-minimal.xml");
    }

    @Test
    public void testJdbcStoreExpressions() throws Exception {
        standardSubsystemTest("jdbc-store-expressions.xml");
    }

    @Test
    public void testParser_EAP_6_4() throws Exception {
        standardSubsystemTest("full-1.5.0.xml");
    }

    @Test
    public void testParser_EAP_7_0() throws Exception {
        standardSubsystemTest("full-3.0.0.xml");
    }

    @Test
    public void testParser_EAP_7_1() throws Exception {
        standardSubsystemTest("full-4.0.0.xml");
    }

    @Test
    public void testParser_EAP_7_2() throws Exception {
        standardSubsystemTest("full-5.0.0.xml");
    }

    @Test
    public void testParser_EAP_7_3() throws Exception {
        standardSubsystemTest("full-5.1.0.xml");
    }

    @Test
    public void testParser_EAP_7_4() throws Exception {
        standardSubsystemTest("full.xml");
    }

    @Test
    public void testTxStats() throws Exception {
        // Parse the subsystem xml and install into the first controller
        final String subsystemXml = getSubsystemXml();
        final KernelServices kernelServices = super.createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
        Assert.assertTrue("Subsystem boot failed!", kernelServices.isSuccessfulBoot());

        // Reads stats
        ModelNode operation = createReadAttributeOperation(CommonAttributes.NUMBER_OF_SYSTEM_ROLLBACKS);
        ModelNode result = kernelServices.executeOperation(operation);
        Assert.assertEquals("success", result.get("outcome").asString());
        Assert.assertEquals(TxStats.getInstance().getNumberOfSystemRollbacks(), result.get(ModelDescriptionConstants.RESULT).asLong());

        operation = createReadAttributeOperation(CommonAttributes.AVERAGE_COMMIT_TIME);
        result = kernelServices.executeOperation(operation);
        Assert.assertEquals("success", result.get("outcome").asString());
        Assert.assertEquals(TxStats.getInstance().getAverageCommitTime(), result.get(ModelDescriptionConstants.RESULT).asLong());
    }

    @Test
    public void testAsyncIOExpressions() throws Exception {
        standardSubsystemTest("async-io-expressions.xml");
    }

    private ModelNode createReadAttributeOperation(String name) {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, getMainSubsystemName());

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.NAME).set(name);
        return operation;
    }
}
