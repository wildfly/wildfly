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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CHECK_FOR_LIVE_SERVER;
import static org.jboss.as.messaging.CommonAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.OVERRIDE_IN_VM_SECURITY;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.HornetQServerResourceDefinition.HORNETQ_SERVER_PATH;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_1;
import static org.jboss.as.messaging.MessagingExtension.VERSION_2_0_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;
import static org.jboss.as.messaging.test.MessagingDependencies.getHornetQDependencies;
import static org.jboss.as.messaging.test.MessagingDependencies.getMessagingGAV;
import static org.jboss.as.messaging.test.ModelFixers.FIXER_1_1_0;
import static org.jboss.as.messaging.test.ModelFixers.FIXER_1_2_0;
import static org.jboss.as.messaging.test.TransformerUtils.concat;
import static org.jboss.as.messaging.test.TransformerUtils.createChainedConfig;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_0_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_0_1;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_1_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_1_1;
import static org.jboss.as.model.test.ModelTestControllerVersion.V7_1_2_FINAL;
import static org.jboss.as.model.test.ModelTestControllerVersion.V7_1_3_FINAL;
import static org.jboss.as.model.test.ModelTestControllerVersion.V7_2_0_FINAL;
import static org.jboss.as.model.test.ModelTestControllerVersion.WILDFLY_8_0_0_FINAL;
import static org.jboss.as.model.test.ModelTestUtils.checkFailedTransformedBootOperations;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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
import org.jboss.as.messaging.HTTPAcceptorDefinition;
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
public class MessagingSubsystem30TestCase extends AbstractSubsystemBaseTest {

    public MessagingSubsystem30TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0_expressions.xml");
    }

    @Test
    public void testTransformersWildFly800() throws Exception {
        testTransformers(WILDFLY_8_0_0_FINAL, VERSION_2_0_0, FIXER_1_1_0);
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
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_1_2_FINAL, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_1_0(V7_1_2_FINAL, builder);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_1_3_FINAL, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_1_0(V7_1_3_FINAL, builder);
    }

    @Test
    public void testRejectExpressionsAS720() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(V7_2_0_FINAL, VERSION_1_2_0, FIXER_1_2_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_2_0(V7_2_0_FINAL, builder);
    }

    @Test
    public void testRejectExpressionsEAP600() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_0_0, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_1_0(EAP_6_0_0, builder);
    }

    @Test
    public void testRejectExpressionsEAP601() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_0_1, VERSION_1_1_0, FIXER_1_1_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_1_0(EAP_6_0_1, builder);
    }

    @Test
    public void testRejectExpressionsEAP610() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_1_0, VERSION_1_2_1, FIXER_1_2_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_2_1(EAP_6_1_0, builder);
    }

    @Test
    public void testRejectExpressionsEAP611() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_1_1, VERSION_1_2_1, FIXER_1_2_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_2_1(EAP_6_1_1, builder);
    }

    @Test
    public void testRejectExpressionsWildFLY800() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(WILDFLY_8_0_0_FINAL, VERSION_2_0_0, FIXER_1_1_0, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_2_0_0(WILDFLY_8_0_0_FINAL, builder);
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
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_3_0_expressions.xml");
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
                                                CommonAttributes.REMOTING_INCOMING_INTERCEPTORS, CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS}, MAX_SAVED_REPLICATED_JOURNAL_SIZE, CHECK_FOR_LIVE_SERVER, OVERRIDE_IN_VM_SECURITY)))
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
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_CONNECTOR)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_CONNECTOR), pathElement(PARAM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_ACCEPTOR)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_ACCEPTOR), pathElement(PARAM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
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
                                        new AttributeDefinition[]{AddressSettingDefinition.EXPIRY_DELAY, AddressSettingDefinition.MAX_REDELIVERY_DELAY, AddressSettingDefinition.REDELIVERY_MULTIPLIER}))
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
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_3_0_expressions.xml");
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
                                        new AttributeDefinition[]{MAX_SAVED_REPLICATED_JOURNAL_SIZE, OVERRIDE_IN_VM_SECURITY}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, BridgeDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE})
                        )
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_CONNECTOR)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_CONNECTOR), pathElement(PARAM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, HTTPAcceptorDefinition.PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, HTTPAcceptorDefinition.PATH, pathElement(PARAM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, GroupingHandlerDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[] {},
                                        new AttributeDefinition[] { GroupingHandlerDefinition.GROUP_TIMEOUT, GroupingHandlerDefinition.REAPER_PERIOD }))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(AddressSettingDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{AddressSettingDefinition.EXPIRY_DELAY, AddressSettingDefinition.MAX_REDELIVERY_DELAY, AddressSettingDefinition.REDELIVERY_MULTIPLIER}))
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
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_3_0_expressions.xml");
        // remove the messaging subsystem add operation that fails on AS7 7.2.0.Final
        modelNodes.remove(0);
        checkFailedTransformedBootOperations(
                mainServices,
                VERSION_1_2_1,
                modelNodes,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{MAX_SAVED_REPLICATED_JOURNAL_SIZE, OVERRIDE_IN_VM_SECURITY}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, BridgeDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_CONNECTOR)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, pathElement(CommonAttributes.HTTP_CONNECTOR), pathElement(PARAM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, HTTPAcceptorDefinition.PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, HTTPAcceptorDefinition.PATH, pathElement(PARAM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH, GroupingHandlerDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[] {},
                                        new AttributeDefinition[] { GroupingHandlerDefinition.GROUP_TIMEOUT, GroupingHandlerDefinition.REAPER_PERIOD }))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(AddressSettingDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{AddressSettingDefinition.EXPIRY_DELAY, AddressSettingDefinition.MAX_REDELIVERY_DELAY, AddressSettingDefinition.REDELIVERY_MULTIPLIER}))
        );
    }

    /**
     * Tests rejection of expressions in 2.0.0 model.
     *
     * @throws Exception
     */
    private void doTestRejectExpressions_2_0_0(ModelTestControllerVersion controllerVersion, KernelServicesBuilder builder) throws Exception {

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_2_0_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());


        //Use the real xml with expressions for testing all the attributes
        PathAddress subsystemAddress = PathAddress.pathAddress(pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME));
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_3_0_expressions.xml");
        modelNodes.remove(0);
        checkFailedTransformedBootOperations(
                mainServices,
                VERSION_2_0_0,
                modelNodes,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{OVERRIDE_IN_VM_SECURITY}))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(AddressSettingDefinition.PATH),
                                createChainedConfig(new AttributeDefinition[]{},
                                        new AttributeDefinition[]{ AddressSettingDefinition.MAX_REDELIVERY_DELAY, AddressSettingDefinition.REDELIVERY_MULTIPLIER }))
        );
    }

    private KernelServicesBuilder createKernelServicesBuilder(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer, String xmlFileName) throws IOException, XMLStreamException, ClassNotFoundException {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(xmlFileName);
        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingGAV(controllerVersion))
                .configureReverseControllerCheck(null, fixer)
                .addMavenResourceURL(getHornetQDependencies(controllerVersion))
                .dontPersistXml();
        return builder;
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_3_0.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingGAV(controllerVersion))
                .configureReverseControllerCheck(null, fixer)
                .addMavenResourceURL(getHornetQDependencies(controllerVersion))
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(messagingVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, messagingVersion);
    }
}
