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
import static org.jboss.as.controller.transform.description.DiscardAttributeChecker.UNDEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.SIMPLE_EXPRESSIONS;
import static org.jboss.as.messaging.AddressSettingDefinition.MAX_REDELIVERY_DELAY;
import static org.jboss.as.messaging.AddressSettingDefinition.REDELIVERY_MULTIPLIER;
import static org.jboss.as.messaging.AddressSettingDefinition.SLOW_CONSUMER_CHECK_PERIOD;
import static org.jboss.as.messaging.AddressSettingDefinition.SLOW_CONSUMER_POLICY;
import static org.jboss.as.messaging.AddressSettingDefinition.SLOW_CONSUMER_THRESHOLD;
import static org.jboss.as.messaging.CommonAttributes.BACKUP_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CHECK_FOR_LIVE_SERVER;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.OVERRIDE_IN_VM_SECURITY;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_INCOMING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS;
import static org.jboss.as.messaging.CommonAttributes.REPLICATION_CLUSTERNAME;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_2_1;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_3_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_2_0_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_2_1_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular;

import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker.DiscardAttributeValueChecker;
import org.jboss.as.controller.transform.description.OperationTransformationOverrideBuilder;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * Resource transformations for the messaging subsystem.
 * <p/>
 * <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc.
 */

public class MessagingTransformers {

    static void registerTransformers(final SubsystemRegistration subsystem) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystem.getSubsystemVersion());

        // Current
        // 3.0.0 -> 2.1.0 (WildFly 8.1.0.Final)
        buildTransformers2_1_0(chainedBuilder.createBuilder(subsystem.getSubsystemVersion(), VERSION_2_1_0));
        // 2.1.0 -> 2.0.0 (WildFly 8.0.0.Final)
        buildTransformers2_0_0(chainedBuilder.createBuilder(VERSION_2_1_0, VERSION_2_0_0));
        // 2.0.0 -> 1.3.0 (AS7 7.3.0)
        buildTransformers1_3_0(chainedBuilder.createBuilder(VERSION_2_0_0, VERSION_1_3_0));
        // 1.3.0 -> 1.2.1 (AS7 7.2.1)
        buildTransformers1_2_1(chainedBuilder.createBuilder(VERSION_1_3_0, VERSION_1_2_1));
        // 1.2.1 -> 1.2.0 (AS7 7.2.0)
        buildTransformers1_2_0(chainedBuilder.createBuilder(VERSION_1_2_1, VERSION_1_2_0));
        // 1.2.0 -> 1.1.0 (AS7 7.1.3, AS7 7.1.2)
        buildTransformers1_1_0(chainedBuilder.createBuilder(VERSION_1_2_0, VERSION_1_1_0));

        chainedBuilder.buildAndRegister(subsystem,  new ModelVersion[]{
                VERSION_1_1_0,
                VERSION_1_2_0,
                VERSION_1_2_1,
                VERSION_1_3_0,
                VERSION_2_0_0,
                VERSION_2_1_0
        });
    }

    /**
     * Transformation for WildFly 8.1.0.Final
     */
    private static void buildTransformers2_1_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
        rejectDefinedAttributeWithDefaultValue(hornetqServer, OVERRIDE_IN_VM_SECURITY);

        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting, MAX_REDELIVERY_DELAY, REDELIVERY_MULTIPLIER, SLOW_CONSUMER_CHECK_PERIOD, SLOW_CONSUMER_POLICY, SLOW_CONSUMER_THRESHOLD);
    }

    /**
     * Transformation for WildFly 8.0.0.Final
     */
    private static void buildTransformers2_0_0(ResourceTransformationDescriptionBuilder builder) {
        // nothing has changed from 8.1.0.Final
    }

    /**
     * Transformation for EAP 6.2.0 / AS7 7.3.0
     */
    private static void buildTransformers1_3_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
        renameAttribute(hornetqServer, CommonAttributes.STATISTICS_ENABLED, CommonAttributes.MESSAGE_COUNTER_ENABLED);

        hornetqServer.rejectChildResource(HTTPAcceptorDefinition.PATH);
        hornetqServer.rejectChildResource(pathElement(CommonAttributes.HTTP_CONNECTOR));

        ResourceTransformationDescriptionBuilder bridge = hornetqServer.addChildResource(BridgeDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(bridge, BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE, BridgeDefinition.INITIAL_CONNECT_ATTEMPTS);

        ResourceTransformationDescriptionBuilder clusterConnection = hornetqServer.addChildResource(ClusterConnectionDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(clusterConnection, ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS);

        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting, AddressSettingDefinition.EXPIRY_DELAY);
    }

    private static void buildTransformers1_2_1(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
        rejectDefinedAttributeWithDefaultValue(hornetqServer, CommonAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE);

        ResourceTransformationDescriptionBuilder bridge = hornetqServer.addChildResource(BridgeDefinition.PATH);
        bridge.getAttributeBuilder().setDiscard(new DiscardAttributeChecker() {
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
                return false;
            }

            @Override
            public boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                return true;
            }
        }, FAILOVER_ON_SERVER_SHUTDOWN);

        ResourceTransformationDescriptionBuilder groupingHandler = hornetqServer.addChildResource(GroupingHandlerDefinition.PATH);
        rejectDefinedAttributeWithDefaultValue(groupingHandler, GroupingHandlerDefinition.GROUP_TIMEOUT, GroupingHandlerDefinition.REAPER_PERIOD);
    }

    private static void buildTransformers1_2_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
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

                        // The 'clustered' attribute is not recognized on 1.3.0. It's only supported for compatibility
                        // with 1.2 or earlier servers, so we discard it for 1.3. For 1.3 servers, see if the desired
                        // value conflicts with the actual configuration, and if so log a transformation warning
                        // before discarding

                        // the real clustered HornetQ state
                        Set<String> clusterConnectionNames = context.readResourceFromRoot(address).getChildrenNames(ClusterConnectionDefinition.PATH.getKey());
                        boolean clustered = !clusterConnectionNames.isEmpty();
                        // whether the user wants the server to be clustered
                        // We use a short-cut vs AD.resolveModelValue to avoid having to hack in an OperationContext
                        // This is ok since the attribute doesn't support expressions
                        // Treat 'undefined' as 'ignore this and match the actual config' instead of the legacy default 'false'
                        boolean wantsClustered = attributeValue.asBoolean(clustered);
                        if (clustered && !wantsClustered) {
                            String msg = MessagingLogger.ROOT_LOGGER.canNotChangeClusteredAttribute(address);
                            context.getLogger().logAttributeWarning(address, operation, msg, CLUSTERED.getName());
                        }
                        return true;
                    }

                    @Override
                    public boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return true;
                    }

                }, CLUSTERED);
    }

    private static void buildTransformers1_1_0(ResourceTransformationDescriptionBuilder builder) {
        // discard JMS bridge resources added in 1.2.0
        builder.rejectChildResource(JMSBridgeDefinition.PATH);

        ResourceTransformationDescriptionBuilder hornetqServer = builder.addChildResource(pathElement(HORNETQ_SERVER));
        rejectAttributesWithExpression(hornetqServer, HornetQServerResourceDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttribute(hornetqServer, BACKUP_GROUP_NAME, REPLICATION_CLUSTERNAME, REMOTING_INCOMING_INTERCEPTORS, REMOTING_OUTGOING_INTERCEPTORS);
        rejectDefinedAttributeWithDefaultValue(hornetqServer, CHECK_FOR_LIVE_SERVER);
        convertUndefinedAttribute(hornetqServer, ID_CACHE_SIZE);

        for (String path : PathDefinition.PATHS.keySet()) {
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

        ResourceTransformationDescriptionBuilder clusterConnection = hornetqServer.addChildResource(ClusterConnectionDefinition.PATH);
        rejectAttributesWithExpression(clusterConnection, ClusterConnectionDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttributeWithDefaultValue(clusterConnection, CALL_FAILOVER_TIMEOUT, ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS, ClusterConnectionDefinition.NOTIFICATION_INTERVAL, ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS);

        ResourceTransformationDescriptionBuilder groupingHandler = hornetqServer.addChildResource(GroupingHandlerDefinition.PATH);
        rejectAttributesWithExpression(groupingHandler, GroupingHandlerDefinition.TYPE, GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS, GroupingHandlerDefinition.TIMEOUT);

        ResourceTransformationDescriptionBuilder addressSetting = hornetqServer.addChildResource(AddressSettingDefinition.PATH);
        rejectAttributesWithExpression(addressSetting, AddressSettingDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);

        ResourceTransformationDescriptionBuilder connectorService = hornetqServer.addChildResource(ConnectorServiceDefinition.PATH);

        ResourceTransformationDescriptionBuilder connectorServiceParam = connectorService.addChildResource(ConnectorServiceParamDefinition.PATH);
        rejectAttributesWithExpression(connectorServiceParam, ConnectorServiceParamDefinition.VALUE);

        ResourceTransformationDescriptionBuilder connectionFactory = hornetqServer.addChildResource(ConnectionFactoryDefinition.PATH);
        rejectAttributesWithExpression(connectionFactory, ConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        rejectDefinedAttributeWithDefaultValue(connectionFactory, CALL_FAILOVER_TIMEOUT);
        convertUndefinedAttribute(connectionFactory, Regular.FACTORY_TYPE);

        ResourceTransformationDescriptionBuilder pooledConnectionFactory = hornetqServer.addChildResource(PooledConnectionFactoryDefinition.PATH);
        rejectAttributesWithExpression(pooledConnectionFactory, PooledConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0);
        convertUndefinedAttribute(pooledConnectionFactory, Pooled.RECONNECT_ATTEMPTS);
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, CALL_FAILOVER_TIMEOUT, Pooled.INITIAL_CONNECT_ATTEMPTS, Common.COMPRESS_LARGE_MESSAGES, Pooled.INITIAL_MESSAGE_PACKET_SIZE,
                Pooled.USE_AUTO_RECOVERY);

        ResourceTransformationDescriptionBuilder jmsQueue = hornetqServer.addChildResource(JMSQueueDefinition.PATH);
        rejectAttributesWithExpression(jmsQueue, CommonAttributes.DESTINATION_ENTRIES, CommonAttributes.SELECTOR, CommonAttributes.DURABLE);

        ResourceTransformationDescriptionBuilder jmsTopic = hornetqServer.addChildResource(JMSTopicDefinition.PATH);
        rejectAttributesWithExpression(jmsTopic, CommonAttributes.DESTINATION_ENTRIES);
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

    /**
     * Rename an attribute
     */
    private static void renameAttribute(ResourceTransformationDescriptionBuilder builder,
                                        AttributeDefinition attribute, AttributeDefinition alias) {
        builder.getAttributeBuilder().addRename(attribute, alias.getName());
    }

}
