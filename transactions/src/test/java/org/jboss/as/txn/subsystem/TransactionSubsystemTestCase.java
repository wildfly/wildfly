/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.subsystem;

import static org.jboss.as.txn.subsystem.TransactionTransformers.MODEL_VERSION_EAP81;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.txn.logging.TransactionLogger;
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
        return "schema/wildfly-txn_7_0.xsd";
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
    public void testParser_EAP_7_4_to_EAP_8_1() throws Exception {
        standardSubsystemTest("full-6.0.0.xml");
    }

    @Test
    public void testParserWildFly40() throws Exception {
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

    @Test
    public void testTransformersFullEAP810() throws Exception {
        testTransformersFull(ModelTestControllerVersion.EAP_8_1_0, MODEL_VERSION_EAP81);
    }

    @Test
    public void testRejectTransformersEAP810() throws Exception {
        testRejectTransformers(ModelTestControllerVersion.EAP_8_1_0, MODEL_VERSION_EAP81,
            new FailedOperationTransformationConfig().addFailedAttribute(
                PathAddress.pathAddress(TransactionExtension.SUBSYSTEM_PATH),
                new FailedOperationTransformationConfig.NewAttributesConfig(
                    TransactionSubsystemRootResourceDefinition.TRANSACTIONS_RECOVERY_GRACEFUL_SHUTDOWN)));
    }

    private void testTransformersFull(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        String subsystemXml = readResource("full.xml");

        // Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
            .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, modelVersion)
            .addMavenResourceURL(controllerVersion.createGAV("wildfly-transactions"))
            .excludeFromParent(SingleClassFilter.createFilter(TransactionLogger.class))
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    private void testRejectTransformers(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, FailedOperationTransformationConfig config) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, modelVersion)
            .addMavenResourceURL(controllerVersion.createGAV("wildfly-transactions"))
            .excludeFromParent(SingleClassFilter.createFilter(TransactionLogger.class))
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("full-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, config);
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
