/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.transform.description.DiscardAttributeChecker.UNDEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.SIMPLE_EXPRESSIONS;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.RUNTIME_QUEUE;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_1;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;

import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Resource transformations for the messaging subsystem.
 * <p/>
 * <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc.
 */
public class MessagingTransformers {

    static void registerTransformers(final SubsystemRegistration subsystem) {
        registerTransformers_1_1_0(subsystem);
        registerTransformers_1_2_0(subsystem);
        registerTransformers_1_2_1(subsystem);
    }

    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {

        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        // discard JMS bridge resources added in 1.2.0
        subsystemRoot.rejectChildResource(JMSBridgeDefinition.PATH);
        // discard runtime resources
        subsystemRoot.rejectChildResource(CoreAddressDefinition.PATH);
        subsystemRoot.rejectChildResource(PathElement.pathElement(RUNTIME_QUEUE));

        ResourceTransformationDescriptionBuilder hornetqServer = subsystemRoot.addChildResource(PathElement.pathElement(HORNETQ_SERVER))
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, HornetQServerResourceDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                        .addRejectCheck(DEFINED, HornetQServerResourceDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .setDiscard(UNDEFINED, HornetQServerResourceDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .setValueConverter(AttributeConverter.Factory.createHardCoded(ID_CACHE_SIZE.getDefaultValue(), true), ID_CACHE_SIZE)
                .end();

        for (String path : MessagingPathHandlers.PATHS.keySet()) {
            hornetqServer.addChildResource(PathElement.pathElement(PATH, path))
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, PATH)
                    .end();
        }

        for (String path : new String[]{CommonAttributes.IN_VM_ACCEPTOR, CommonAttributes.IN_VM_CONNECTOR}) {
            final ResourceTransformationDescriptionBuilder transport = hornetqServer.addChildResource(PathElement.pathElement(path))
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, InVMTransportDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                    .end()
                    .addOperationTransformationOverride(ADD)
                            .inheritResourceAttributeDefinitions()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, CommonAttributes.PARAM) // additional attribute to the ADD operation
                    .end();
            transport.addChildResource(TransportParamDefinition.PATH)
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                    .end();
        }

        for (String path : new String[]{CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.REMOTE_CONNECTOR, CommonAttributes.ACCEPTOR, CommonAttributes.CONNECTOR}) {
            final ResourceTransformationDescriptionBuilder transport = hornetqServer.addChildResource(PathElement.pathElement(path))
                    .addOperationTransformationOverride(ADD)
                            .inheritResourceAttributeDefinitions()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, CommonAttributes.PARAM) // additional attribute to the ADD operation
                    .end();
            transport.addChildResource(TransportParamDefinition.PATH)
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                    .end();
        }

        hornetqServer.rejectChildResource(ServletConnectorDefinition.PATH);

        hornetqServer.addChildResource(BroadcastGroupDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, BroadcastGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, BroadcastGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, BroadcastGroupDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(DiscoveryGroupDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, DiscoveryGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, DiscoveryGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, DiscoveryGroupDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(DivertDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, DivertDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();


        hornetqServer.addChildResource(QueueDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, QueueDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(BridgeDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, BridgeDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(ClusterConnectionDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, ClusterConnectionDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, ClusterConnectionDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, ClusterConnectionDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(GroupingHandlerDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, GroupingHandlerDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(AddressSettingDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, AddressSettingDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        ResourceTransformationDescriptionBuilder connectorService = hornetqServer.addChildResource(ConnectorServiceDefinition.PATH);

        connectorService.addChildResource(ConnectorServiceParamDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, ConnectorServiceParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(ConnectionFactoryDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, ConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, ConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, ConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(PooledConnectionFactoryDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, PooledConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, PooledConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, PooledConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                        .setValueConverter(AttributeConverter.Factory.createHardCoded(Pooled.RECONNECT_ATTEMPTS.getDefaultValue(), true), Pooled.RECONNECT_ATTEMPTS)
                .end();

        hornetqServer.addChildResource(JMSQueueDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, JMSQueueDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(JMSTopicDefinition.PATH)
                .getAttributeBuilder()
                .addRejectCheck(SIMPLE_EXPRESSIONS, JMSTopicDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        TransformationDescription.Tools.register(subsystemRoot.build(), subsystem, VERSION_1_1_0);
    }

    private static void registerTransformers_1_2_0(final SubsystemRegistration subsystem) {

        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder hornetqServer = subsystemRoot.addChildResource(PathElement.pathElement(HORNETQ_SERVER));
        hornetqServer.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker() {
                    @Override
                    public boolean isDiscardExpressions() {
                        return false;
                    }

                    @Override
                    public boolean isDiscardUndefined() {
                        return true;
                    }

                    @Override
                    public boolean isOperationParameterDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {

                        // The 'clustered' attribute is not recognized on 1.4.0. It's only supported for compatibility
                        // with 1.3 or earlier servers, so we discard it for 1.4. For 1.4 servers, see if the desired
                        // value conflicts with the actual configuration, and if so log a transformation warning
                        // before discarding

                        // the real clustered HornetQ state
                        Set<String> clusterConnectionNames = context.readResource(PathAddress.EMPTY_ADDRESS).getChildrenNames(ClusterConnectionDefinition.PATH.getKey());
                        boolean clustered = !clusterConnectionNames.isEmpty();
                        // whether the user wants the server to be clustered
                        // We use a short-cut vs AD.resolveModelValue to avoid having to hack in an OperationContext
                        // This is ok since the attribute doesn't support expressions
                        // Treat 'undefined' as 'ignore this and match the actual config' instead of the legacy default 'false'
                        boolean wantsClustered = attributeValue.asBoolean(clustered);
                        if (clustered && !wantsClustered) {
                            PathAddress serverAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
                            String msg = MessagingMessages.MESSAGES.canNotChangeClusteredAttribute(serverAddress);
                            context.getLogger().logAttributeWarning(serverAddress, operation, msg, CLUSTERED.getName());
                        }
                        return true;
                    }

                    @Override
                    public boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return true;
                    }

                }, CommonAttributes.CLUSTERED)
                .end();

        hornetqServer.rejectChildResource(ServletConnectorDefinition.PATH);

        TransformationDescription.Tools.register(subsystemRoot.build(), subsystem, VERSION_1_2_0);
    }

    private static void registerTransformers_1_2_1(final SubsystemRegistration subsystem) {

        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        ResourceTransformationDescriptionBuilder hornetqServer = subsystemRoot.addChildResource(PathElement.pathElement(HORNETQ_SERVER));

        hornetqServer.addChildResource(BridgeDefinition.PATH)
                .getAttributeBuilder()
                .setDiscard(UNDEFINED, BridgeDefinition.ATTRIBUTES_ADDED_IN_2_0_0)
                .addRejectCheck(DEFINED, BridgeDefinition.ATTRIBUTES_ADDED_IN_2_0_0)
                .end();

        hornetqServer.addChildResource(AddressSettingDefinition.PATH)
                .getAttributeBuilder()
                .setDiscard(UNDEFINED, AddressSettingDefinition.ATTRIBUTES_ADDED_IN_2_0_0)
                .addRejectCheck(DEFINED, AddressSettingDefinition.ATTRIBUTES_ADDED_IN_2_0_0)
                .end();

        hornetqServer.rejectChildResource(ServletConnectorDefinition.PATH);

        TransformationDescription.Tools.register(subsystemRoot.build(), subsystem, VERSION_1_2_1);
    }
}
