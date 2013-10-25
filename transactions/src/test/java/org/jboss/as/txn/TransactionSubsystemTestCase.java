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
package org.jboss.as.txn;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.BINDING;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.ENABLE_STATISTICS;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.HORNETQ_STORE_ENABLE_ASYNC_IO;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.PATH;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.RELATIVE_TO;
import static org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition.STATUS_BINDING;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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
import org.jboss.as.txn.subsystem.TransactionExtension;
import org.jboss.as.txn.subsystem.TransactionSubsystemRootResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
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
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
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
    public void testParser_1_2() throws Exception {
        standardSubsystemTest("full-1.2.xml");
    }

    @Test
    public void testParser_1_3() throws Exception {
        standardSubsystemTest("full-1.3.xml");
    }

    @Test
    public void testAsyncIOExpressions() throws Exception {
        standardSubsystemTest("async-io-expressions.xml");
    }


    @Test
    public void testTransformersFullAS712() throws Exception {
        testTransformersFull110(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    private void testTransformersFull110(ModelTestControllerVersion controllerVersion) throws Exception {
        String subsystemXml = readResource("full.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
            .addMavenResourceURL("org.jboss.as:jboss-as-transactions:" + controllerVersion.getMavenGavVersion())
            .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, ADD_REMOVED_HORNETQ_STORE_ENABLE_ASYNC_IO)
            .excludeFromParent(SingleClassFilter.createFilter(TransactionLogger.class));

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);

        final ModelNode writeAttribute = new ModelNode();
        writeAttribute.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeAttribute.get(OP_ADDR).add("subsystem", "transactions");
        writeAttribute.get(NAME).set("use-jdbc-store");
        writeAttribute.get(VALUE).set(false);

        final OperationTransformer.TransformedOperation op = mainServices.transformOperation(modelVersion, writeAttribute);
        Assert.assertNotNull(op);
        Assert.assertNotNull(op.getTransformedOperation());
    }


    @Test
    public void testTransformersFull713() throws Exception {
        testTransformersFull(ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 1, 1));
    }

    @Test
    public void testTransformersFull720() throws Exception {
        testTransformersFull(ModelTestControllerVersion.V7_2_0_FINAL, ModelVersion.create(1, 2, 0));
    }

    private void testTransformersFull(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        String subsystemXml = readResource("full-expressions.xml");
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);

        final PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, mainSubsystemName));
        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-transactions:" + controllerVersion.getMavenGavVersion())
                .addOperationValidationResolve(ADD, subsystemAddress)
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, ADD_REMOVED_HORNETQ_STORE_ENABLE_ASYNC_IO)
                .excludeFromParent(SingleClassFilter.createFilter(TransactionLogger.class));

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);

    }

    @Test
    public void testRejectTransformersAS712() throws Exception {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()
        .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME)),
                new FailedOperationTransformationConfig.ChainedConfig(Arrays.asList(new FailedOperationTransformationConfig.AttributesPathAddressConfig<?>[] {
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                DEFAULT_TIMEOUT,
                                ENABLE_STATISTICS,
                                ENABLE_TSM_STATUS,
                                BINDING,
                                STATUS_BINDING,
                                RECOVERY_LISTENER,
                                NODE_IDENTIFIER,
                                PATH,
                                RELATIVE_TO,
                                PROCESS_ID_SOCKET_BINDING,
                                PROCESS_ID_SOCKET_MAX_PORTS,
                                OBJECT_STORE_PATH,
                                OBJECT_STORE_RELATIVE_TO
                                ),
                        new ChangeToTrueConfig(HORNETQ_STORE_ENABLE_ASYNC_IO)
                }) ,
                        DEFAULT_TIMEOUT,
                        ENABLE_STATISTICS,
                        ENABLE_TSM_STATUS,
                        BINDING,
                        STATUS_BINDING,
                        RECOVERY_LISTENER,
                        NODE_IDENTIFIER,
                        PATH,
                        RELATIVE_TO,
                        PROCESS_ID_SOCKET_BINDING,
                        PROCESS_ID_SOCKET_MAX_PORTS,
                        OBJECT_STORE_PATH,
                        OBJECT_STORE_RELATIVE_TO,
                        HORNETQ_STORE_ENABLE_ASYNC_IO));
        testRejectTransformers(ModelTestControllerVersion.V7_1_2_FINAL, ModelVersion.create(1, 1, 0), config);
    }

    @Test
    public void testRejectTransformers713() throws Exception {
        testRejectTransformers(ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 1, 1), new FailedOperationTransformationConfig()
            .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME)),
                    new ChangeToTrueConfig(HORNETQ_STORE_ENABLE_ASYNC_IO)));

    }

    @Test
    public void testRejectTransformers720() throws Exception {
        testRejectTransformers(ModelTestControllerVersion.V7_1_3_FINAL, ModelVersion.create(1, 2, 0), new FailedOperationTransformationConfig()
            .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME)),
                    new ChangeToTrueConfig(HORNETQ_STORE_ENABLE_ASYNC_IO)));
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

        List<ModelNode> ops = builder.parseXmlResource("full-expressions-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, config);
    }

    private static ModelFixer ADD_REMOVED_HORNETQ_STORE_ENABLE_ASYNC_IO = new ModelFixer() {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            modelNode.get(TransactionSubsystemRootResourceDefinition.HORNETQ_STORE_ENABLE_ASYNC_IO.getName()).set(true);
            return modelNode;
        }
    };

    private static final class ChangeToTrueConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<ChangeToTrueConfig>{

        public ChangeToTrueConfig(AttributeDefinition...attributeDefinitions) {
            super(convert(attributeDefinitions));
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.asString().equals("true");
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(true);
        }

    }
}
