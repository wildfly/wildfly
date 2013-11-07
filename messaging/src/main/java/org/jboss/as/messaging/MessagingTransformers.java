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

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.transform.description.DiscardAttributeChecker.DiscardAttributeValueChecker;
import static org.jboss.as.controller.transform.description.DiscardAttributeChecker.UNDEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.SIMPLE_EXPRESSIONS;
import static org.jboss.as.messaging.AddressSettingDefinition.EXPIRY_DELAY;
import static org.jboss.as.messaging.BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE;
import static org.jboss.as.messaging.ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS;
import static org.jboss.as.messaging.ClusterConnectionDefinition.NOTIFICATION_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.BACKUP_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CHECK_FOR_LIVE_SERVER;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_INCOMING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.REPLICATION_CLUSTERNAME;
import static org.jboss.as.messaging.GroupingHandlerDefinition.GROUP_TIMEOUT;
import static org.jboss.as.messaging.GroupingHandlerDefinition.REAPER_PERIOD;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_1;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_3_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.INITIAL_CONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.INITIAL_MESSAGE_PACKET_SIZE;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_AUTO_RECOVERY;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;

import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.OperationTransformationOverrideBuilder;
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
        registerTransformers_1_3_0(subsystem);
    }

    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {

        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        // discard JMS bridge resources added in 1.2.0
        subsystemRoot.rejectChildResource(JMSBridgeDefinition.PATH);

        ResourceTransformationDescriptionBuilder hornetqServer = subsystemRoot.addChildResource(pathElement(HORNETQ_SERVER));
        rejectAttributesWithExpression(hornetqServer, HornetQServerResourceDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttribute(hornetqServer, BACKUP_GROUP_NAME, REPLICATION_CLUSTERNAME, REMOTING_INCOMING_INTERCEPTORS, REMOTING_OUTGOING_INTERCEPTORS);
        rejectDefinedAttributeWithDefaultValue(hornetqServer, CHECK_FOR_LIVE_SERVER, MAX_SAVED_REPLICATED_JOURNAL_SIZE);
        convertUndefinedAttribute(hornetqServer, ID_CACHE_SIZE);

        for (String path : MessagingPathHandlers.PATHS.keySet()) {
            ResourceTransformationDescriptionBuilder serverPaths = hornetqServer.addChildResource(pathElement(PATH, path));
            rejectAttributesWithExpression(serverPaths, PATH);
        }

        for (String path : new String[] { CommonAttributes.IN_VM_ACCEPTOR, CommonAttributes.IN_VM_CONNECTOR }) {
            ResourceTransformationDescriptionBuilder transport = hornetqServer.addChildResource(pathElement(path));
            rejectAttributesWithExpression(transport, InVMTransportDefinition.SERVER_ID);

            OperationTransformationOverrideBuilder transportAddOp = transport.addOperationTransformationOverride(ADD).inheritResourceAttributeDefinitions();
            rejectAttributesWithExpression(transportAddOp, CommonAttributes.PARAM);

            ResourceTransformationDescriptionBuilder transportParam = transport.addChildResource(TransportParamDefinition.PATH);
            rejectAttributesWithExpression(transportParam, TransportParamDefinition.VALUE);
        }

        for (String path : new String[] { CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.REMOTE_CONNECTOR, CommonAttributes.ACCEPTOR, CommonAttributes.CONNECTOR }) {
            ResourceTransformationDescriptionBuilder transport = hornetqServer.addChildResource(pathElement(path));

            OperationTransformationOverrideBuilder transportAddOp = transport.addOperationTransformationOverride(ADD).inheritResourceAttributeDefinitions();
            rejectAttributesWithExpression(transportAddOp, CommonAttributes.PARAM);

            ResourceTransformationDescriptionBuilder transportParam = transport.addChildResource(TransportParamDefinition.PATH);
            rejectAttributesWithExpression(transportParam, TransportParamDefinition.VALUE);
        }

        ResourceTransformationDescriptionBuilder broadcastGroup = hornetqServer.addChildResource(BroadcastGroupDefinition.PATH);
        rejectAttributesWithExpression(broadcastGroup, BroadcastGroupDefinition.BROADCAST_PERIOD);
        rejectDefinedAttribute(broadcastGroup, CommonAttributes.JGROUPS_CHANNEL, CommonAttributes.JGROUPS_STACK);


        ResourceTransformationDescriptionBuilder discoveryGroup = hornetqServer.addChildResource(DiscoveryGroupDefinition.PATH);
        rejectAttributesWithExpression(discoveryGroup, DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT, DiscoveryGroupDefinition.REFRESH_TIMEOUT);
        rejectDefinedAttribute(discoveryGroup, CommonAttributes.JGROUPS_CHANNEL, CommonAttributes.JGROUPS_STACK);


        ResourceTransformationDescriptionBuilder divert = hornetqServer.addChildResource(DivertDefinition.PATH);
        rejectAttributesWithExpression(divert, DivertDefinition.ROUTING_NAME, DivertDefinition.ADDRESS, DivertDefinition.FORWARDING_ADDRESS, CommonAttributes.FILTER,
                DivertDefinition.EXCLUSIVE);

        ResourceTransformationDescriptionBuilder queue = hornetqServer.addChildResource(QueueDefinition.PATH);
        rejectAttributesWithExpression(queue, QueueDefinition.ADDRESS, CommonAttributes.FILTER, CommonAttributes.DURABLE);

        ResourceTransformationDescriptionBuilder bridge = hornetqServer.addChildResource(BridgeDefinition.PATH);
        rejectAttributesWithExpression(bridge, BridgeDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttributeWithDefaultValue(bridge, RECONNECT_ATTEMPTS_ON_SAME_NODE);

        ResourceTransformationDescriptionBuilder clusterConnection = hornetqServer.addChildResource(ClusterConnectionDefinition.PATH);
        rejectAttributesWithExpression(clusterConnection, ClusterConnectionDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttributeWithDefaultValue(clusterConnection, CALL_FAILOVER_TIMEOUT, NOTIFICATION_ATTEMPTS, NOTIFICATION_INTERVAL);

        ResourceTransformationDescriptionBuilder groupingHandler = hornetqServer.addChildResource(GroupingHandlerDefinition.PATH);
        rejectAttributesWithExpression(groupingHandler, GroupingHandlerDefinition.TYPE, GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS, GroupingHandlerDefinition.TIMEOUT);
        rejectDefinedAttributeWithDefaultValue(groupingHandler, GROUP_TIMEOUT, REAPER_PERIOD);

        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectAttributesWithExpression(addressSetting, AddressSettingDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttributeWithDefaultValue(addressSetting, EXPIRY_DELAY);

        ResourceTransformationDescriptionBuilder connectorService = hornetqServer.addChildResource(ConnectorServiceDefinition.PATH);

        ResourceTransformationDescriptionBuilder connectorServiceParam = connectorService.addChildResource(ConnectorServiceParamDefinition.PATH);
        rejectAttributesWithExpression(connectorServiceParam, ConnectorServiceParamDefinition.VALUE);

        ResourceTransformationDescriptionBuilder connectionFactory = hornetqServer.addChildResource(ConnectionFactoryDefinition.PATH);
        rejectAttributesWithExpression(connectionFactory, ConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttributeWithDefaultValue(connectionFactory, CALL_FAILOVER_TIMEOUT);
        convertUndefinedAttribute(connectionFactory, FACTORY_TYPE);

        ResourceTransformationDescriptionBuilder pooledConnectionFactory = hornetqServer.addChildResource(PooledConnectionFactoryDefinition.PATH);
        rejectAttributesWithExpression(pooledConnectionFactory, PooledConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        convertUndefinedAttribute(pooledConnectionFactory, Pooled.RECONNECT_ATTEMPTS);
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, CALL_FAILOVER_TIMEOUT, INITIAL_CONNECT_ATTEMPTS, COMPRESS_LARGE_MESSAGES, INITIAL_MESSAGE_PACKET_SIZE,
                USE_AUTO_RECOVERY);

        ResourceTransformationDescriptionBuilder jmsQueue = hornetqServer.addChildResource(JMSQueueDefinition.PATH);
        rejectAttributesWithExpression(jmsQueue, CommonAttributes.DESTINATION_ENTRIES, CommonAttributes.SELECTOR, CommonAttributes.DURABLE);

        ResourceTransformationDescriptionBuilder jmsTopic = hornetqServer.addChildResource(JMSTopicDefinition.PATH);
        rejectAttributesWithExpression(jmsTopic, CommonAttributes.DESTINATION_ENTRIES);

        TransformationDescription.Tools.register(subsystemRoot.build(), subsystem, VERSION_1_1_0);
    }

    private static void registerTransformers_1_2_0(final SubsystemRegistration subsystem) {

        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder hornetqServer = subsystemRoot.addChildResource(pathElement(HORNETQ_SERVER));
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
                        Set<String> clusterConnectionNames = context.readResource(address).getChildrenNames(ClusterConnectionDefinition.PATH.getKey());
                        boolean clustered = !clusterConnectionNames.isEmpty();
                        // whether the user wants the server to be clustered
                        // We use a short-cut vs AD.resolveModelValue to avoid having to hack in an OperationContext
                        // This is ok since the attribute doesn't support expressions
                        // Treat 'undefined' as 'ignore this and match the actual config' instead of the legacy default 'false'
                        boolean wantsClustered = attributeValue.asBoolean(clustered);
                        if (clustered && !wantsClustered) {
                            String msg = MessagingMessages.MESSAGES.canNotChangeClusteredAttribute(address);
                            context.getLogger().logAttributeWarning(address, operation, msg, CLUSTERED.getName());
                        }
                        return true;
                    }

                    @Override
                    public boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return true;
                    }

                }, CommonAttributes.CLUSTERED)
                .end();
        rejectDefinedAttributeWithDefaultValue(hornetqServer, MAX_SAVED_REPLICATED_JOURNAL_SIZE);

        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting, EXPIRY_DELAY);

        ResourceTransformationDescriptionBuilder bridge = hornetqServer.addChildResource(BridgeDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(bridge, RECONNECT_ATTEMPTS_ON_SAME_NODE);
        // the FAILOVER_ON_SERVER_SHUTDOWN attribute has been deprecated in 1.2.0
        // but it was erroneously removed from the model. Discard it always just for 1.2.0
        discardAttribute(bridge, FAILOVER_ON_SERVER_SHUTDOWN);

        ResourceTransformationDescriptionBuilder groupingHandler = hornetqServer.addChildResource(GroupingHandlerDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(groupingHandler, GROUP_TIMEOUT, REAPER_PERIOD);

        TransformationDescription.Tools.register(subsystemRoot.build(), subsystem, VERSION_1_2_0);
    }

    private static void registerTransformers_1_2_1(final SubsystemRegistration subsystem) {

        TransformationDescription.Tools.register(get1_2_1_1_3_0Description(), subsystem, VERSION_1_2_1);
    }

    private static void registerTransformers_1_3_0(final SubsystemRegistration subsystem) {

        TransformationDescription.Tools.register(get1_2_1_1_3_0Description(), subsystem, VERSION_1_3_0);
    }

    private static TransformationDescription get1_2_1_1_3_0Description() {
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        ResourceTransformationDescriptionBuilder hornetqServer = subsystemRoot.addChildResource(pathElement(HORNETQ_SERVER));
        rejectDefinedAttributeWithDefaultValue(hornetqServer, MAX_SAVED_REPLICATED_JOURNAL_SIZE);

        ResourceTransformationDescriptionBuilder bridge = hornetqServer.addChildResource(BridgeDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(bridge, RECONNECT_ATTEMPTS_ON_SAME_NODE);
        discardAttribute(bridge, FAILOVER_ON_SERVER_SHUTDOWN);

        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting, EXPIRY_DELAY);

        ResourceTransformationDescriptionBuilder groupingHandler = hornetqServer.addChildResource(GroupingHandlerDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(groupingHandler, GROUP_TIMEOUT, REAPER_PERIOD);

        return subsystemRoot.build();
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined.
     */
    private static void rejectDefinedAttribute(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .setDiscard(UNDEFINED, attr)
                    .addRejectCheck(DEFINED, attr);
        }
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeValueChecker(attr.getDefaultValue()), attr)
                    .addRejectCheck(DEFINED, attr);
        }
    }

    /**
     * Reject the attributes if they hold an expression.
     */
    private static void rejectAttributesWithExpression(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        builder.getAttributeBuilder().addRejectCheck(SIMPLE_EXPRESSIONS, attrs);
    }

    /**
     * Reject the attributes if they hold an expression.
     */
    private static void rejectAttributesWithExpression(ResourceTransformationDescriptionBuilder builder, String... attrs) {
        builder.getAttributeBuilder().addRejectCheck(SIMPLE_EXPRESSIONS, attrs);
    }

    /**
     * Reject the operation's attributes if they hold an expression.
     */
    private static void rejectAttributesWithExpression(OperationTransformationOverrideBuilder operation, String... attrs) {
        operation.addRejectCheck(SIMPLE_EXPRESSIONS, attrs);
    }

    /**
     * Convert the attributes by hard-coding them to the default values if they are not defined.
     */
    private static void convertUndefinedAttribute(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder().setValueConverter(AttributeConverter.Factory.createHardCoded(attr.getDefaultValue(), true), attr);
        }
    }

    /**
     * Always discard the attributes.
     */
    private static void discardAttribute(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, attrs);
    }

}
