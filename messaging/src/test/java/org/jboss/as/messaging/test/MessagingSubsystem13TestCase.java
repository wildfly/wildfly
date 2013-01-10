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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.*;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.messaging.HornetQServerResourceDefinition.HORNETQ_SERVER_PATH;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import static org.jboss.as.model.test.FailedOperationTransformationConfig.NewAttributesConfig;
import static org.jboss.as.model.test.ModelTestUtils.checkFailedTransformedBootOperations;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
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
import org.jboss.as.messaging.HornetQServerResourceDefinition;
import org.jboss.as.messaging.InVMTransportDefinition;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.QueueDefinition;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc

 */
public class MessagingSubsystem13TestCase extends AbstractSubsystemBaseTest {

    public MessagingSubsystem13TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    /*
     * test 1.3-only features. Compatible features are tested in #testTransformers()
     */
    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_incompatible_1_3.xml");
    }

    @Test
    public void testTransformers_1_1_0() throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_1_3.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), VERSION_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.2.Final")
                .addMavenResourceURL("org.hornetq:hornetq-core:2.2.16.Final")
                .addMavenResourceURL("org.hornetq:hornetq-jms:2.2.16.Final")
                .addMavenResourceURL("org.hornetq:hornetq-ra:2.2.16.Final");
        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(VERSION_1_1_0).isSuccessfulBoot());
    }

    /**
     * Tests rejection of expressions in 1.1.0 model.
     *
     * @throws Exception
     */
    @Test
    public void testRejectExpressions_1_1_0() throws Exception {
        // create builder for current subsystem version.
        //
        // AS7 7.1.2.Final does not allow to add an empty messaging subsystem [AS7-5767]
        // To work around that, we add an empty "stuff" hornetq-server to boot the conf with AS7 7.1.2.Final
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("empty_subsystem_1_3.xml");

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), VERSION_1_1_0)
                .addMavenResourceURL("org.hornetq:hornetq-core:2.2.16.Final")
                .addMavenResourceURL("org.hornetq:hornetq-jms:2.2.16.Final")
                .addMavenResourceURL("org.hornetq:hornetq-ra:2.2.16.Final")
                .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.2.Final")
                .addMavenResourceURL("org.jboss.as:jboss-as-controller:7.1.2.Final")
                .addParentFirstClassPattern("org.jboss.as.controller.*");

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_1_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        //Use the real xml with expressions for testing all the attributes
        PathAddress subsystemAddress = PathAddress.pathAddress(pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME));
        List<ModelNode> modelNodes = builder.parseXmlResource("subsystem_incompatible_1_3.xml");
        // remote the messaging subsystem add operation that fails on AS7 7.1.2.Final
        modelNodes.remove(0);
        checkFailedTransformedBootOperations(
                mainServices,
                VERSION_1_1_0,
                modelNodes,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH),
                                new RejectExpressionsConfig(HornetQServerResourceDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(ModelDescriptionConstants.PATH)),
                                new RejectExpressionsConfig(ModelDescriptionConstants.PATH))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.IN_VM_CONNECTOR)),
                                new RejectExpressionsConfig(InVMTransportDefinition.SERVER_ID))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.CONNECTOR)),
                                new RejectExpressionsConfig(CommonAttributes.FACTORY_CLASS))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.IN_VM_ACCEPTOR)),
                                new RejectExpressionsConfig(InVMTransportDefinition.SERVER_ID))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.ACCEPTOR)),
                                new RejectExpressionsConfig(CommonAttributes.FACTORY_CLASS))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(BroadcastGroupDefinition.PATH),
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
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(DivertDefinition.PATH),
                                new RejectExpressionsConfig(DivertDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(QueueDefinition.PATH),
                                new RejectExpressionsConfig(QueueDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(ClusterConnectionDefinition.PATH),
                                new RejectExpressionsConfig(ClusterConnectionDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(BridgeDefinition.PATH),
                                new RejectExpressionsConfig(BridgeDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(GroupingHandlerDefinition.PATH),
                                new RejectExpressionsConfig(GroupingHandlerDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(AddressSettingDefinition.PATH),
                                new RejectExpressionsConfig(AddressSettingDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(ConnectorServiceDefinition.PATH),
                                new RejectExpressionsConfig(CommonAttributes.FACTORY_CLASS))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(ConnectorServiceDefinition.PATH).append(ConnectorServiceParamDefinition.PATH),
                                new RejectExpressionsConfig(ConnectorServiceParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(ConnectionFactoryDefinition.PATH),
                                new RejectExpressionsConfig(ConnectionFactoryDefinition.REJECTED_EXPRESSION_ATTRIBUTES)
                                        .setReadOnly(FACTORY_TYPE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(PooledConnectionFactoryDefinition.PATH),
                                new ChainedConfig(new HashMap<String, FailedOperationTransformationConfig.PathAddressConfig>() {{
                                    for (AttributeDefinition attr : PooledConnectionFactoryDefinition.REJECTED_EXPRESSION_ATTRIBUTES) {
                                            put(attr.getName(), new RejectExpressionsConfig(attr));
                                    }
                                    //put(USE_AUTO_RECOVERY.getName(), new NewAttributesConfig(USE_AUTO_RECOVERY));
                                    //put(INITIAL_CONNECT_ATTEMPTS.getName(), new NewAttributesConfig(INITIAL_CONNECT_ATTEMPTS));
                                    //put(INITIAL_MESSAGE_PACKET_SIZE.getName(), new NewAttributesConfig(INITIAL_MESSAGE_PACKET_SIZE));
                                    //put(COMPRESS_LARGE_MESSAGES.getName(), new NewAttributesConfig(COMPRESS_LARGE_MESSAGES));
                                    //put(CALL_FAILOVER_TIMEOUT.getName(), new NewAttributesConfig(CALL_FAILOVER_TIMEOUT));
                                }}).setReadOnly(ConnectionFactoryAttributes.Pooled.TRANSACTION))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(JMSQueueDefinition.PATH),
                                new RejectExpressionsConfig(JMSQueueDefinition.REJECTED_EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(
                                subsystemAddress.append(JMSBridgeDefinition.PATH),
                                new RejectExpressionsConfig(new String[0]) {
                                    @Override
                                    public boolean expectFailed(ModelNode operation) {
                                        // jms-bridge resource was introduced in version 1.2.0
                                        return true;
                                    }
                                })
        );
    }
}
