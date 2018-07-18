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

import com.arjuna.ats.arjuna.coordinator.TxStats;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.jboss.as.txn.subsystem.TransactionTransformers.MODEL_VERSION_EAP64;
import static org.jboss.as.txn.subsystem.TransactionTransformers.MODEL_VERSION_EAP70;
import static org.jboss.as.txn.subsystem.TransactionTransformers.MODEL_VERSION_EAP71;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


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
        return "schema/wildfly-txn_5_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/transactions.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
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

    private static ModelNode success() {
        final ModelNode result = new ModelNode();
        result.get(ModelDescriptionConstants.OUTCOME).set(ModelDescriptionConstants.SUCCESS);
        result.get(ModelDescriptionConstants.RESULT);
        return result;
    }

    @Test
    public void testAsyncIOExpressions() throws Exception {
        standardSubsystemTest("async-io-expressions.xml");
    }

    @Test
    public void testTransformersFullEAP640() throws Exception {
        testTransformersFull(ModelTestControllerVersion.EAP_6_4_0, MODEL_VERSION_EAP64);
    }

    @Test
    public void testTransformersFullEAP700() throws Exception {
        testTransformersFull(ModelTestControllerVersion.EAP_7_0_0, MODEL_VERSION_EAP70);
    }

    @Test
    public void testTransformersFullEAP710() throws Exception {
        testTransformersFull(ModelTestControllerVersion.EAP_7_1_0, MODEL_VERSION_EAP71);
    }

    private void testTransformersFull(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        String subsystemXml = readResource(String.format("full-%s.xml", modelVersion));

        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        final String artifactId = controllerVersion == ModelTestControllerVersion.EAP_6_4_0 ?
                "jboss-as-transactions" : "wildfly-transactions";
        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL(String.format("%s:%s:%s",
                        controllerVersion.getMavenGroupId(), artifactId, controllerVersion.getMavenGavVersion()))
                .excludeFromParent(SingleClassFilter.createFilter(TransactionLogger.class));

        if (controllerVersion == ModelTestControllerVersion.EAP_6_4_0) {
            initializer.addSingleChildFirstClass(RemoveProcessUUIDOperationFixer.class)
                    .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, ADD_REMOVED_HORNETQ_STORE_ENABLE_ASYNC_IO, RemoveProcessUUIDOperationFixer.INSTANCE);
        }

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelFixer modelFixer = null;
        switch (controllerVersion) {
            case EAP_6_4_0:
                modelFixer = new ModelFixer() {
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        modelNode.remove("path");
                        modelNode.remove("relative-to");
                        return modelNode;
                    }
                };
                break;
            case EAP_7_0_0:
            case EAP_7_1_0:
                modelFixer = new ModelFixer() {
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        if (modelNode.has("log-store", "log-store")) {
                            ModelNode logStore = modelNode.get("log-store", "log-store");
                            if (!logStore.get("expose-all-logs").asBoolean()) {
                                logStore.remove("expose-all-logs");
                            }
                        }
                        return modelNode;
                    }
                };
                break;

        }
        checkSubsystemModelTransformation(mainServices, modelVersion, modelFixer);
    }

    @Test
    public void testRejectTransformersEAP640() throws Exception {
        testRejectTransformers(ModelTestControllerVersion.EAP_6_4_0, MODEL_VERSION_EAP64, new FailedOperationTransformationConfig().addFailedAttribute(
                PathAddress.pathAddress(TransactionExtension.SUBSYSTEM_PATH), new FailedOperationTransformationConfig.NewAttributesConfig("maximum-timeout")));
    }

    @Test
    public void testRejectTransformersEAP700() throws Exception {
        testRejectTransformers7(ModelTestControllerVersion.EAP_7_0_0, MODEL_VERSION_EAP70, new FailedOperationTransformationConfig().addFailedAttribute(
                PathAddress.pathAddress(TransactionExtension.SUBSYSTEM_PATH), new FailedOperationTransformationConfig.NewAttributesConfig("maximum-timeout")));
    }

    @Test
    public void testRejectTransformersEAP710() throws Exception {
        testRejectTransformers7(ModelTestControllerVersion.EAP_7_1_0, MODEL_VERSION_EAP71, new FailedOperationTransformationConfig().addFailedAttribute(
                PathAddress.pathAddress(TransactionExtension.SUBSYSTEM_PATH), new FailedOperationTransformationConfig.NewAttributesConfig("maximum-timeout")));
    }

    private void testRejectTransformers7(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, FailedOperationTransformationConfig config) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.eap:wildfly-transactions:" + controllerVersion.getMavenGavVersion())
                .excludeFromParent(SingleClassFilter.createFilter(TransactionLogger.class));

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("full-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, config);

        if (modelVersion == MODEL_VERSION_EAP70) {
            PathAddress subsystemAddress = PathAddress.pathAddress(TransactionExtension.SUBSYSTEM_PATH);
            PathAddress participants = subsystemAddress.append(TransactionExtension.LOG_STORE_PATH).append(TransactionExtension.TRANSACTION_PATH).append(TransactionExtension.PARTICIPANT_PATH);
            //check that we reject log-store=log-store/transactions=*/participants=*:delete
            OperationTransformer.TransformedOperation transOp = mainServices.transformOperation(modelVersion, Util.createOperation("delete", participants));
            Assert.assertTrue(transOp.getFailureDescription(), transOp.rejectOperation(success()));
        }

    }

    private void testRejectTransformers(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion, FailedOperationTransformationConfig config) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-transactions:" + controllerVersion.getMavenGavVersion())
                .excludeFromParent(SingleClassFilter.createFilter(TransactionLogger.class));

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

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

    private static ModelFixer ADD_REMOVED_HORNETQ_STORE_ENABLE_ASYNC_IO = modelNode -> {
        modelNode.get(TransactionSubsystemRootResourceDefinition.HORNETQ_STORE_ENABLE_ASYNC_IO.getName()).set(true);
        modelNode.get(TransactionSubsystemRootResourceDefinition.JOURNAL_STORE_ENABLE_ASYNC_IO.getName()).set(true);
        modelNode.get(TransactionSubsystemRootResourceDefinition.USE_HORNETQ_STORE.getName()).set(true);
        modelNode.get(TransactionSubsystemRootResourceDefinition.USE_JOURNAL_STORE.getName()).set(true);
        return modelNode;
    };
}
