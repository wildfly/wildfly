/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.test;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.messaging.CommonAttributes.BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CHECK_FOR_LIVE_SERVER;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.HornetQServerResourceDefinition.HORNETQ_SERVER_PATH;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_1;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;
import static org.jboss.as.messaging.test.TransformerUtils.concat;
import static org.jboss.as.messaging.test.TransformerUtils.createChainedConfig;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_0_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_0_1;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_1_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_1_1;
import static org.jboss.as.model.test.ModelTestControllerVersion.V7_1_2_FINAL;
import static org.jboss.as.model.test.ModelTestControllerVersion.V7_1_3_FINAL;
import static org.jboss.as.model.test.ModelTestControllerVersion.V7_2_0_FINAL;
import static org.jboss.as.model.test.ModelTestUtils.checkFailedTransformedBootOperations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.messaging.AddressSettingDefinition;
import org.jboss.as.messaging.BridgeDefinition;
import org.jboss.as.messaging.BroadcastGroupDefinition;
import org.jboss.as.messaging.ClusterConnectionDefinition;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.ConnectorServiceDefinition;
import org.jboss.as.messaging.ConnectorServiceParamDefinition;
import org.jboss.as.messaging.DiscoveryGroupDefinition;
import org.jboss.as.messaging.DivertDefinition;
import org.jboss.as.messaging.GroupingHandlerDefinition;
import org.jboss.as.messaging.HornetQServerResourceDefinition;
import org.jboss.as.messaging.InVMTransportDefinition;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.QueueDefinition;
import org.jboss.as.messaging.TransportParamDefinition;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.as.messaging.test.TransformerUtils.RejectExpressionsConfigWithAddOnlyParam;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class MessagingSubsystem20TestCase extends AbstractSubsystemBaseTest {

    public MessagingSubsystem20TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_2_0.xml");
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("subsystem_2_0_expressions.xml");
    }

    @Test
    public void testTransformersAS720() throws Exception {
        testTransformers(V7_2_0_FINAL, VERSION_1_2_0, FIXER_1_2_0);
    }

    @Test
    public void testTransformersAS712() throws Exception {
        testTransformers(V7_1_2_FINAL, VERSION_1_1_0, FIXER_1_1_0);
    }

    @Test
    public void testTransformersAS713() throws Exception {
        testTransformers(V7_1_3_FINAL, VERSION_1_1_0, FIXER_1_1_0);
    }

    @Test
    public void testTransformersEAP600() throws Exception {
        testTransformers(EAP_6_0_0, VERSION_1_1_0, FIXER_1_1_0);
    }

    @Test
    public void testTransformersEAP601() throws Exception {
        testTransformers(EAP_6_0_1, VERSION_1_1_0, FIXER_1_1_0);
    }

    @Test
    public void testTransformersEAP610() throws Exception {
        testTransformers(EAP_6_1_0, VERSION_1_2_1, FIXER_1_2_0);
    }

    @Test
    public void testTransformersEAP611() throws Exception {
        testTransformers(EAP_6_1_1, VERSION_1_2_1, FIXER_1_2_0);
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_1_2_FINAL, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_2_0.xml");

        doTestRejectExpressions_1_1_0(V7_1_2_FINAL, builder);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_1_3_FINAL, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_2_0.xml");

        doTestRejectExpressions_1_1_0(V7_1_3_FINAL, builder);
    }

    @Test
    public void testRejectExpressionsAS720() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_2_0_FINAL, VERSION_1_2_0, FIXER_1_2_0, "empty_subsystem_2_0.xml");

        doTestRejectExpressions_1_2_0(V7_2_0_FINAL, builder);
    }

    @Test
    public void testRejectExpressionsEAP600() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_0_0, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_2_0.xml");

        doTestRejectExpressions_1_1_0(EAP_6_0_0, builder);
    }

    @Test
    public void testRejectExpressionsEAP601() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_0_1, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_2_0.xml");

        doTestRejectExpressions_1_1_0(EAP_6_0_1, builder);
    }

    @Test
    public void testRejectExpressionsEAP610() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_1_0, VERSION_1_2_1, FIXER_1_2_0, "empty_subsystem_2_0.xml");

        doTestRejectExpressions_1_2_1(EAP_6_1_0, builder);
    }

    @Test
    public void testRejectExpressionsEAP611() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_1_1, VERSION_1_2_1, FIXER_1_2_0, "empty_subsystem_2_0.xml");

        doTestRejectExpressions_1_2_1(EAP_6_1_1, builder);
    }

    @Test
    public void testClusteredTo120() throws Exception {
        // this hornetq-server has 1 cluster-connection resource defined and so is clustered.
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_2_0_FINAL, VERSION_1_2_0, FIXER_1_2_0, "subsystem_with_cluster_connection_2_0.xml");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_2_0);
        assertNotNull(legacyServices);
        assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());

        clusteredTo120Test(VERSION_1_2_0, mainServices, true);
        clusteredTo120Test(VERSION_1_2_0, mainServices, false);
    }

    private void clusteredTo120Test(ModelVersion version120, KernelServices mainServices, boolean clustered) throws OperationFailedException {

        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME),
                // stuff if the empty hornetq-server defined in empty_subsystem_2_0
                PathElement.pathElement(HornetQServerResourceDefinition.HORNETQ_SERVER_PATH.getKey(), "stuff"));

        ModelNode addOp = Util.createAddOperation(pa);
        addOp.get(CommonAttributes.CLUSTERED.getName()).set(clustered);

        OperationTransformer.TransformedOperation transformedOperation = mainServices.transformOperation(version120, addOp);
        assertFalse(transformedOperation.getTransformedOperation().has(CommonAttributes.CLUSTERED.getName()));

        ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        assertFalse(transformedOperation.rejectOperation(result));
        assertEquals(result, transformedOperation.transformResult(result));

        ModelNode writeOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, pa);
        writeOp.get(NAME).set(CommonAttributes.CLUSTERED.getName());
        writeOp.get(VALUE).set(clustered);

        transformedOperation = mainServices.transformOperation(version120, writeOp);
        assertNull(transformedOperation.getTransformedOperation());
    }

    /**
     * Tests rejection of expressions in 1.1.0 model.
     *
     * @throws Exception
     */
    private void doTestRejectExpressions_1_1_0(ModelTestControllerVersion controllerVersion, KernelServicesBuilder builder) throws Exception {
        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_1_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());


        //Use the real xml with expressions for testing all the attributes
        PathAddress subsystemAddress = PathAddress.pathAddress(pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME));
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_2_0_expressions.xml");
        // remote the messaging subsystem add operation that fails on AS7 7.1.2.Final
        modelNodes.remove(0);
        checkFailedTransformedBootOperations(
                mainServices,
                VERSION_1_1_0,
                modelNodes,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH),
                                createChainedConfig(
                                        HornetQServerResourceDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        concat(new AttributeDefinition[]{CommonAttributes.BACKUP_GROUP_NAME, CommonAttributes.REPLICATION_CLUSTERNAME,
                                                CommonAttributes.REMOTING_INCOMING_INTERCEPTORS, CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS}, MAX_SAVED_REPLICATED_JOURNAL_SIZE, CHECK_FOR_LIVE_SERVER)))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(ModelDescriptionConstants.PATH)),
                                new RejectExpressionsConfig(ModelDescriptionConstants.PATH))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.IN_VM_ACCEPTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(concat(new AttributeDefinition[]{InVMTransportDefinition.SERVER_ID}, PARAM)))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.IN_VM_ACCEPTOR)).append(TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.IN_VM_CONNECTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(concat(new AttributeDefinition[]{InVMTransportDefinition.SERVER_ID}, PARAM)))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.IN_VM_CONNECTOR), TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.REMOTE_ACCEPTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.REMOTE_ACCEPTOR), TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.REMOTE_CONNECTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.REMOTE_CONNECTOR), TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.ACCEPTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.ACCEPTOR), TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.CONNECTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.CONNECTOR), TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, BroadcastGroupDefinition.PATH),
                                new RejectExpressionsConfig(BroadcastGroupDefinition.BROADCAST_PERIOD) {
                                    @Override
                                    public boolean expectFailed(ModelNode operation) {
                                        if ("groupT".equals(operation.get(OP_ADDR).get(2).get(CommonAttributes.BROADCAST_GROUP).asString())) {
                                            // groupT use JGroups and do not define socket-binding or local address
                                            return true;
                                        }
                                        return super.expectFailed(operation);
                                    }
                                })
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(DiscoveryGroupDefinition.PATH),
                                new RejectExpressionsConfig(DiscoveryGroupDefinition.REFRESH_TIMEOUT, DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT) {
                                    @Override
                                    public boolean expectFailed(ModelNode operation) {
                                        if ("groupU".equals(operation.get(OP_ADDR).get(2).get(CommonAttributes.DISCOVERY_GROUP).asString())) {
                                            // groupU use JGroups and do not define socket-binding or local address
                                            return true;
                                        }
                                        return super.expectFailed(operation);
                                    }
                                })
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, DivertDefinition.PATH),
                                new RejectExpressionsConfig(DivertDefinition.ROUTING_NAME, DivertDefinition.ADDRESS, DivertDefinition.FORWARDING_ADDRESS, CommonAttributes.FILTER,
                                        DivertDefinition.EXCLUSIVE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, QueueDefinition.PATH),
                                new RejectExpressionsConfig(QueueDefinition.ADDRESS, CommonAttributes.FILTER, CommonAttributes.DURABLE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, ClusterConnectionDefinition.PATH),
                                createChainedConfig(ClusterConnectionDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        new AttributeDefinition[]{CommonAttributes.CALL_FAILOVER_TIMEOUT, ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS,
                                                ClusterConnectionDefinition.NOTIFICATION_INTERVAL}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, BridgeDefinition.PATH),
                                createChainedConfig(BridgeDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        new AttributeDefinition[]{BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, GroupingHandlerDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[] { GroupingHandlerDefinition.TYPE, GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS, GroupingHandlerDefinition.TIMEOUT },
                                        new AttributeDefinition[] { GroupingHandlerDefinition.GROUP_TIMEOUT, GroupingHandlerDefinition.REAPER_PERIOD }))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, AddressSettingDefinition.PATH),
                                createChainedConfig(AddressSettingDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        new AttributeDefinition[]{AddressSettingDefinition.EXPIRY_DELAY}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, ConnectorServiceDefinition.PATH, ConnectorServiceParamDefinition.PATH),
                                new RejectExpressionsConfig(ConnectorServiceParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, ConnectionFactoryDefinition.PATH),
                                createChainedConfig(ConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        new AttributeDefinition[]{CALL_FAILOVER_TIMEOUT}).setReadOnly(FACTORY_TYPE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, PooledConnectionFactoryDefinition.PATH),
                                createChainedConfig(PooledConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        concat(PooledConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0, CALL_FAILOVER_TIMEOUT))
                                .setReadOnly(ConnectionFactoryAttributes.Pooled.TRANSACTION))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, JMSQueueDefinition.PATH),
                                new RejectExpressionsConfig(CommonAttributes.DESTINATION_ENTRIES, CommonAttributes.SELECTOR, CommonAttributes.DURABLE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, JMSTopicDefinition.PATH),
                                new RejectExpressionsConfig(CommonAttributes.DESTINATION_ENTRIES))
                        .addFailedAttribute(
                                subsystemAddress.append(JMSBridgeDefinition.PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
        );
    }


    /**
     * Tests rejection of expressions in 1.2.0 model.
     *
     * @throws Exception
     */
    private void doTestRejectExpressions_1_2_0(ModelTestControllerVersion controllerVersion, KernelServicesBuilder builder) throws Exception {

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_2_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());


        //Use the real xml with expressions for testing all the attributes
        PathAddress subsystemAddress = PathAddress.pathAddress(pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME));
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_2_0_expressions.xml");
        // remove the messaging subsystem add operation that fails on AS7 7.2.0.Final
        modelNodes.remove(0);
        checkFailedTransformedBootOperations(
                mainServices,
                VERSION_1_2_0,
                modelNodes,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{MAX_SAVED_REPLICATED_JOURNAL_SIZE}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, BridgeDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, GroupingHandlerDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[] {},
                                        new AttributeDefinition[] { GroupingHandlerDefinition.GROUP_TIMEOUT, GroupingHandlerDefinition.REAPER_PERIOD }))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(AddressSettingDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{AddressSettingDefinition.EXPIRY_DELAY}))
        );
    }


    /**
     * Tests rejection of expressions in 1.2.1 model.
     *
     * @throws Exception
     */
    private void doTestRejectExpressions_1_2_1(ModelTestControllerVersion controllerVersion, KernelServicesBuilder builder) throws Exception {

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_2_1);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());


        //Use the real xml with expressions for testing all the attributes
        PathAddress subsystemAddress = PathAddress.pathAddress(pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME));
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_2_0_expressions.xml");
        // remove the messaging subsystem add operation that fails on AS7 7.2.0.Final
        modelNodes.remove(0);
        checkFailedTransformedBootOperations(
                mainServices,
                VERSION_1_2_1,
                modelNodes,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, BridgeDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(AddressSettingDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{AddressSettingDefinition.EXPIRY_DELAY}))
        );
    }

    private KernelServicesBuilder createKernelServicesBuilder(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer, String xmlFileName) throws IOException, XMLStreamException, ClassNotFoundException {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(xmlFileName);
        // create builder for legacy subsystem version
        HornetQDependencies.addDependencies(controllerVersion,
                builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                        .addMavenResourceURL("org.jboss.as:jboss-as-messaging:" + controllerVersion.getMavenGavVersion())
                        .configureReverseControllerCheck(null, fixer));
        return builder;
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_2_0.xml");
        HornetQDependencies.addDependencies(controllerVersion,
                builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                        .addMavenResourceURL("org.jboss.as:jboss-as-messaging:" + controllerVersion.getMavenGavVersion())
                        .configureReverseControllerCheck(null, fixer));

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(messagingVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, messagingVersion);
    }

    private static final ModelFixer FIXER_1_1_0 = new ModelFixer() {
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // Since AS7-5417, messaging's paths resources are always created.
            // however for legacy version, they were only created if the path attributes were different from the defaults.
            // The 'empty' hornetq-server does not set any messaging's path so we discard them to "fix" the model and
            // compare the current and legacy versions
            for (String serverWithDefaultPath  : new String[] {"empty", "stuff"}) {
                if (modelNode.get(HORNETQ_SERVER).has(serverWithDefaultPath)) {
                    modelNode.get(HORNETQ_SERVER, serverWithDefaultPath, PATH).set(new ModelNode());
                }
            }
            return modelNode;
        }
    };

    private static final ModelFixer FIXER_1_2_0 = new ModelFixer() {
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // Since AS7-5417, messaging's paths resources are always created.
            // however for legacy version, they were only created if the path attributes were different from the defaults.
            // The 'empty' hornetq-server does not set any messaging's path so we discard them to "fix" the model and
            // compare the current and legacy versions
            for (String serverWithDefaultPath  : new String[] {"empty", "stuff"}) {
                if (modelNode.get(HORNETQ_SERVER).has(serverWithDefaultPath)) {
                    modelNode.get(HORNETQ_SERVER, serverWithDefaultPath, PATH).set(new ModelNode());
                }
            }
            if (modelNode.get(HORNETQ_SERVER).has("default")) {
                modelNode.get(HORNETQ_SERVER, "default", BRIDGE, "bridge1", FAILOVER_ON_SERVER_SHUTDOWN.getName()).set(true);
            }
            return modelNode;
        }
    };
}
