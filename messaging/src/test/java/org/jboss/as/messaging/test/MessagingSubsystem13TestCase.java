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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.HornetQServerResourceDefinition.HORNETQ_SERVER_PATH;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;
import static org.jboss.as.model.test.ModelTestUtils.checkFailedTransformedBootOperations;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
import org.jboss.as.messaging.TransportParamDefinition;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.NewAttributesConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;
import org.jboss.as.model.test.ModelFixer;
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
    public void testTransformersAS712() throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_1_3.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), VERSION_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.2.Final")
                .addMavenResourceURL("org.hornetq:hornetq-core:2.2.16.Final")
                .addMavenResourceURL("org.hornetq:hornetq-jms:2.2.16.Final")
                .addMavenResourceURL("org.hornetq:hornetq-ra:2.2.16.Final")
                .configureReverseControllerCheck(null, DEFAULT_PATH_FIXER);

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(VERSION_1_1_0).isSuccessfulBoot());
    }

    @Test
    public void testTransformersAS713() throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_1_3.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), VERSION_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.3.Final")
                .addMavenResourceURL("org.hornetq:hornetq-core:2.2.21.Final")
                .addMavenResourceURL("org.hornetq:hornetq-jms:2.2.21.Final")
                .addMavenResourceURL("org.hornetq:hornetq-ra:2.2.21.Final")
                .configureReverseControllerCheck(null, DEFAULT_PATH_FIXER);


        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(VERSION_1_1_0).isSuccessfulBoot());
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
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
                .addParentFirstClassPattern("org.jboss.as.controller.*")
                .configureReverseControllerCheck(null, DEFAULT_PATH_FIXER);

        doTestRejectExpressions_1_1_0(builder);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        // AS7 7.1.3.Final does not allow to add an empty messaging subsystem [AS7-5767]
        // To work around that, we add an empty "stuff" hornetq-server to boot the conf with AS7 7.1.2.Final
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("empty_subsystem_1_3.xml");

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), VERSION_1_1_0)
                .addMavenResourceURL("org.hornetq:hornetq-core:2.2.21.Final")
                .addMavenResourceURL("org.hornetq:hornetq-jms:2.2.21.Final")
                .addMavenResourceURL("org.hornetq:hornetq-ra:2.2.21.Final")
                .addMavenResourceURL("org.jboss.as:jboss-as-messaging:7.1.3.Final")
                .addMavenResourceURL("org.jboss.as:jboss-as-controller:7.1.3.Final")
                .addParentFirstClassPattern("org.jboss.as.controller.*")
                .configureReverseControllerCheck(null, DEFAULT_PATH_FIXER);

        doTestRejectExpressions_1_1_0(builder);
    }

    private static class RejectExpressionsConfigWithAddOnlyParam extends RejectExpressionsConfig {

        RejectExpressionsConfigWithAddOnlyParam(String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            if (PARAM.equals(attributeName)) {
                return false;
            }
            return super.isAttributeWritable(attributeName);
        }
    }

    /**
     * Tests rejection of expressions in 1.1.0 model.
     *
     * @throws Exception
     */
    private void doTestRejectExpressions_1_1_0(KernelServicesBuilder builder) throws Exception {
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
                                createChainedConfig(
                                        HornetQServerResourceDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        HornetQServerResourceDefinition.ATTRIBUTES_ADDED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(ModelDescriptionConstants.PATH)),
                                new RejectExpressionsConfig(ModelDescriptionConstants.PATH))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.IN_VM_ACCEPTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(concat(InVMTransportDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0, PARAM)))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.IN_VM_ACCEPTOR)).append(TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.IN_VM_CONNECTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(concat(InVMTransportDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0, PARAM)))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.IN_VM_CONNECTOR)).append(TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.REMOTE_ACCEPTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.REMOTE_ACCEPTOR)).append(TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.REMOTE_CONNECTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.REMOTE_CONNECTOR)).append(TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.ACCEPTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.ACCEPTOR)).append(TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.CONNECTOR)),
                                new RejectExpressionsConfigWithAddOnlyParam(PARAM))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(pathElement(CommonAttributes.CONNECTOR)).append(TransportParamDefinition.PATH),
                                new RejectExpressionsConfig(TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
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
                                new RejectExpressionsConfig(DivertDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(QueueDefinition.PATH),
                                new RejectExpressionsConfig(QueueDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(ClusterConnectionDefinition.PATH),
                                createChainedConfig(ClusterConnectionDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        ClusterConnectionDefinition.ATTRIBUTES_ADDED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(BridgeDefinition.PATH),
                                new RejectExpressionsConfig(BridgeDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(GroupingHandlerDefinition.PATH),
                                new RejectExpressionsConfig(GroupingHandlerDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(AddressSettingDefinition.PATH),
                                new RejectExpressionsConfig(AddressSettingDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(ConnectorServiceDefinition.PATH).append(ConnectorServiceParamDefinition.PATH),
                                new RejectExpressionsConfig(ConnectorServiceParamDefinition.VALUE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(ConnectionFactoryDefinition.PATH),
                                createChainedConfig(ConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        new AttributeDefinition[]{CALL_FAILOVER_TIMEOUT}).setReadOnly(FACTORY_TYPE))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(PooledConnectionFactoryDefinition.PATH),
                                createChainedConfig(PooledConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0,
                                        PooledConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0).setReadOnly(ConnectionFactoryAttributes.Pooled.TRANSACTION))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(JMSQueueDefinition.PATH),
                                new RejectExpressionsConfig(JMSQueueDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(HORNETQ_SERVER_PATH).append(JMSTopicDefinition.PATH),
                                new RejectExpressionsConfig(JMSTopicDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0))
                        .addFailedAttribute(
                                subsystemAddress.append(JMSBridgeDefinition.PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }


    private static String[] concat(AttributeDefinition[] attrs1, String... attrs2) {
        String[] newAttrs = new String[attrs1.length + attrs2.length];
        for(int i = 0; i < attrs1.length; i++) {
            newAttrs[i] = attrs1[i].getName();
        }
        for(int i = 0; i < attrs2.length; i++) {
            newAttrs[attrs1.length + i] = attrs2[i];
        }
        return newAttrs;
    }

    private static ChainedConfig createChainedConfig(AttributeDefinition[] rejectedExpression, AttributeDefinition[] newAttributes) {
        AttributeDefinition[] allAttributes = new AttributeDefinition[rejectedExpression.length + newAttributes.length];
        System.arraycopy(rejectedExpression, 0, allAttributes, 0, rejectedExpression.length);
        System.arraycopy(newAttributes, 0, allAttributes, rejectedExpression.length, newAttributes.length);

        return ChainedConfig.createBuilder(allAttributes)
            .addConfig(new RejectExpressionsConfig(rejectedExpression))
            .addConfig(new NewAttributesConfig(newAttributes)).build();
    }

    private static final ModelFixer DEFAULT_PATH_FIXER = new ModelFixer() {
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
}
