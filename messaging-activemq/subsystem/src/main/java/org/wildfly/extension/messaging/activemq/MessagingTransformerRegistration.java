/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.security.CredentialReference.REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.UNDEFINED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.IN_VM_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REMOTE_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.EXTERNAL_JMS_QUEUE_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.EXTERNAL_JMS_TOPIC_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.QUEUE_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SERVER_PATH;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.messaging.activemq.ha.HAAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicControlHandler;
import org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition;

/**
 * {@link org.jboss.as.controller.transform.ExtensionTransformerRegistration} for the messaging-activemq subsystem.
 *
 * @author Paul Ferraro
 */
@MetaInfServices
public class MessagingTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return MessagingExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        registerTransformers_WF_23(builder.createBuilder(MessagingExtension.VERSION_13_0_0, MessagingExtension.VERSION_12_0_0));
        registerTransformers_WF_22(builder.createBuilder(MessagingExtension.VERSION_12_0_0, MessagingExtension.VERSION_11_0_0));
        registerTransformers_WF_21(builder.createBuilder(MessagingExtension.VERSION_11_0_0, MessagingExtension.VERSION_10_0_0));
        registerTransformers_WF_20(builder.createBuilder(MessagingExtension.VERSION_10_0_0, MessagingExtension.VERSION_9_0_0));
        registerTransformers_WF_19(builder.createBuilder(MessagingExtension.VERSION_9_0_0, MessagingExtension.VERSION_8_0_0));
        registerTransformers_WF_18(builder.createBuilder(MessagingExtension.VERSION_8_0_0, MessagingExtension.VERSION_7_0_0));
        registerTransformers_WF_17(builder.createBuilder(MessagingExtension.VERSION_7_0_0, MessagingExtension.VERSION_6_0_0));
        registerTransformers_WF_16(builder.createBuilder(MessagingExtension.VERSION_6_0_0, MessagingExtension.VERSION_5_0_0));
        registerTransformers_WF_15(builder.createBuilder(MessagingExtension.VERSION_5_0_0, MessagingExtension.VERSION_4_0_0));
        registerTransformers_EAP_7_2_0(builder.createBuilder(MessagingExtension.VERSION_4_0_0, MessagingExtension.VERSION_3_0_0));
        registerTransformers_EAP_7_1_0(builder.createBuilder(MessagingExtension.VERSION_3_0_0, MessagingExtension.VERSION_2_0_0));
        registerTransformers_EAP_7_0_0(builder.createBuilder(MessagingExtension.VERSION_2_0_0, MessagingExtension.VERSION_1_0_0));

        builder.buildAndRegister(registration, new ModelVersion[]{MessagingExtension.VERSION_1_0_0, MessagingExtension.VERSION_2_0_0,
            MessagingExtension.VERSION_3_0_0, MessagingExtension.VERSION_4_0_0, MessagingExtension.VERSION_5_0_0,
            MessagingExtension.VERSION_6_0_0, MessagingExtension.VERSION_7_0_0, MessagingExtension.VERSION_8_0_0,
            MessagingExtension.VERSION_9_0_0, MessagingExtension.VERSION_10_0_0, MessagingExtension.VERSION_11_0_0,
            MessagingExtension.VERSION_12_0_0, MessagingExtension.VERSION_13_0_0});
    }

    private static void registerTransformers_WF_23(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);

        rejectDefinedAttributeWithDefaultValue(server,
                ServerDefinition.NETWORK_CHECK_LIST,
                ServerDefinition.NETWORK_CHECK_NIC,
                ServerDefinition.NETWORK_CHECK_PERIOD,
                ServerDefinition.NETWORK_CHECK_PING6_COMMAND,
                ServerDefinition.NETWORK_CHECK_PING_COMMAND,
                ServerDefinition.NETWORK_CHECK_TIMEOUT,
                ServerDefinition.NETWORK_CHECK_URL_LIST,
                ServerDefinition.CRITICAL_ANALYZER_CHECK_PERIOD,
                ServerDefinition.CRITICAL_ANALYZER_ENABLED,
                ServerDefinition.CRITICAL_ANALYZER_POLICY,
                ServerDefinition.CRITICAL_ANALYZER_TIMEOUT,
                ServerDefinition.JOURNAL_MAX_ATTIC_FILES);
        server.discardOperations(PrintDataOperation.OPERATION_NAME);

        ResourceTransformationDescriptionBuilder bridge = server.addChildResource(MessagingExtension.BRIDGE_PATH);
        rejectDefinedAttributeWithDefaultValue(bridge, BridgeDefinition.CALL_TIMEOUT);
        bridge.getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE)
                .end();

        ResourceTransformationDescriptionBuilder clusterConnection = server.addChildResource(MessagingExtension.CLUSTER_CONNECTION_PATH);
        clusterConnection.getAttributeBuilder()
                .setValueConverter(AttributeConverter.DEFAULT_VALUE, CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE)
                .end();
    }

    private static void registerTransformers_WF_22(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.NETWORK_CHECK_LIST);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.NETWORK_CHECK_NIC);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.NETWORK_CHECK_PERIOD);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.NETWORK_CHECK_PING6_COMMAND);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.NETWORK_CHECK_PING_COMMAND);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.NETWORK_CHECK_TIMEOUT);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.NETWORK_CHECK_URL_LIST);
        server.addChildResource(MessagingExtension.JMS_TOPIC_PATH).discardOperations(JMSTopicControlHandler.PAUSE, JMSTopicControlHandler.RESUME);
        server.addChildResource(MessagingExtension.JMS_TOPIC_PATH).getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, CommonAttributes.PAUSED).end();
    }

    private static void registerTransformers_WF_21(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder externalConnectionFactory = subsystem.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(externalConnectionFactory,
                ConnectionFactoryAttributes.Common.CLIENT_FAILURE_CHECK_PERIOD,
                ConnectionFactoryAttributes.Common.CONNECTION_TTL,
                CommonAttributes.CALL_TIMEOUT,
                CommonAttributes.CALL_FAILOVER_TIMEOUT,
                ConnectionFactoryAttributes.Common.CONSUMER_WINDOW_SIZE,
                ConnectionFactoryAttributes.Common.CONSUMER_MAX_RATE,
                ConnectionFactoryAttributes.Common.CONFIRMATION_WINDOW_SIZE,
                ConnectionFactoryAttributes.Common.PRODUCER_WINDOW_SIZE,
                ConnectionFactoryAttributes.Common.PRODUCER_MAX_RATE,
                ConnectionFactoryAttributes.Common.PROTOCOL_MANAGER_FACTORY,
                ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES,
                ConnectionFactoryAttributes.Common.CACHE_LARGE_MESSAGE_CLIENT,
                CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
                CommonAttributes.CLIENT_ID,
                ConnectionFactoryAttributes.Common.DUPS_OK_BATCH_SIZE,
                ConnectionFactoryAttributes.Common.TRANSACTION_BATCH_SIZE,
                ConnectionFactoryAttributes.Common.BLOCK_ON_ACKNOWLEDGE,
                ConnectionFactoryAttributes.Common.BLOCK_ON_NON_DURABLE_SEND,
                ConnectionFactoryAttributes.Common.BLOCK_ON_DURABLE_SEND,
                ConnectionFactoryAttributes.Common.AUTO_GROUP,
                ConnectionFactoryAttributes.Common.PRE_ACKNOWLEDGE,
                ConnectionFactoryAttributes.Common.RETRY_INTERVAL,
                ConnectionFactoryAttributes.Common.RETRY_INTERVAL_MULTIPLIER,
                CommonAttributes.MAX_RETRY_INTERVAL,
                ConnectionFactoryAttributes.Common.RECONNECT_ATTEMPTS,
                ConnectionFactoryAttributes.Common.FAILOVER_ON_INITIAL_CONNECTION,
                ConnectionFactoryAttributes.Common.CONNECTION_LOAD_BALANCING_CLASS_NAME,
                ConnectionFactoryAttributes.Common.USE_GLOBAL_POOLS,
                ConnectionFactoryAttributes.Common.SCHEDULED_THREAD_POOL_MAX_SIZE,
                ConnectionFactoryAttributes.Common.THREAD_POOL_MAX_SIZE,
                ConnectionFactoryAttributes.Common.GROUP_ID,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST,
                ConnectionFactoryAttributes.Common.INITIAL_MESSAGE_PACKET_SIZE);
    }

    private static void registerTransformers_WF_20(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);
        server.getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, ServerDefinition.CREDENTIAL_REFERENCE.getName())
                .end();
        ResourceTransformationDescriptionBuilder bridge = server.addChildResource(MessagingExtension.BRIDGE_PATH);
        bridge.getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, BridgeDefinition.CREDENTIAL_REFERENCE.getName())
                .end();

        ResourceTransformationDescriptionBuilder jmsBridge = subsystem.addChildResource(MessagingExtension.JMS_BRIDGE_PATH);
        jmsBridge.getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, JMSBridgeDefinition.SOURCE_CREDENTIAL_REFERENCE.getName())
                .end();
        jmsBridge.getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, JMSBridgeDefinition.TARGET_CREDENTIAL_REFERENCE.getName())
                .end();

        ResourceTransformationDescriptionBuilder pooledConnectionFactory = server.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        pooledConnectionFactory.getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, ConnectionFactoryAttributes.Pooled.CREDENTIAL_REFERENCE)
                .end();
    }

    private static void registerTransformers_WF_19(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.addChildResource(DiscoveryGroupDefinition.PATH).setCustomResourceTransformer(ResourceTransformer.DISCARD);
        subsystem.addChildRedirection(JGroupsDiscoveryGroupDefinition.PATH, DiscoveryGroupDefinition.PATH);
        subsystem.addChildRedirection(SocketDiscoveryGroupDefinition.PATH, DiscoveryGroupDefinition.PATH);
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);
        server.addChildRedirection(MessagingExtension.JGROUPS_BROADCAST_GROUP_PATH, MessagingExtension.BROADCAST_GROUP_PATH);
        server.addChildRedirection(MessagingExtension.SOCKET_BROADCAST_GROUP_PATH, MessagingExtension.BROADCAST_GROUP_PATH);
        server.addChildRedirection(JGroupsDiscoveryGroupDefinition.PATH, DiscoveryGroupDefinition.PATH);
        server.addChildRedirection(SocketDiscoveryGroupDefinition.PATH, DiscoveryGroupDefinition.PATH);
        server.addChildResource(DiscoveryGroupDefinition.PATH).setCustomResourceTransformer(ResourceTransformer.DISCARD);
        server.addChildResource(MessagingExtension.BROADCAST_GROUP_PATH).setCustomResourceTransformer(ResourceTransformer.DISCARD);
    }

    private static void registerTransformers_WF_18(ResourceTransformationDescriptionBuilder subsystem) {
    }

    private static void registerTransformers_WF_17(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder externalConnectionFactory = subsystem.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(externalConnectionFactory, ConnectionFactoryAttributes.Common.USE_TOPOLOGY, ConnectionFactoryAttributes.External.ENABLE_AMQ1_PREFIX);
        ResourceTransformationDescriptionBuilder externalPooledConnectionFactory = subsystem.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(externalPooledConnectionFactory, ConnectionFactoryAttributes.Common.USE_TOPOLOGY, ConnectionFactoryAttributes.External.ENABLE_AMQ1_PREFIX);

        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(SERVER_PATH);
        ResourceTransformationDescriptionBuilder connectionFactory = server.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(connectionFactory, ConnectionFactoryAttributes.Common.USE_TOPOLOGY);
        ResourceTransformationDescriptionBuilder pooledConnectionFactory = server.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Common.USE_TOPOLOGY);

        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.JOURNAL_FILE_OPEN_TIMEOUT);
    }

    private static void registerTransformers_WF_16(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder queue = subsystem.addChildResource(SERVER_PATH).addChildResource(QUEUE_PATH);
        rejectDefinedAttributeWithDefaultValue(queue, QueueDefinition.ROUTING_TYPE);

        ResourceTransformationDescriptionBuilder jmsBridge = subsystem.addChildResource(MessagingExtension.JMS_BRIDGE_PATH);
        jmsBridge.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, JMSBridgeDefinition.QUALITY_OF_SERVICE, JMSBridgeDefinition.FAILURE_RETRY_INTERVAL, JMSBridgeDefinition.MAX_RETRIES, JMSBridgeDefinition.MAX_BATCH_SIZE, JMSBridgeDefinition.MAX_BATCH_TIME);
    }

    private static void registerTransformers_WF_15(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);
        // WFLY-10976 - journal-pool-files default value is 10.
        server.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, ServerDefinition.JOURNAL_POOL_FILES);
        rejectDefinedAttributeWithDefaultValue(server,
                ServerDefinition.GLOBAL_MAX_DISK_USAGE,
                ServerDefinition.DISK_SCAN_PERIOD,
                ServerDefinition.GLOBAL_MAX_MEMORY_SIZE);
    }

    private static void registerTransformers_EAP_7_2_0(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.rejectChildResource(DiscoveryGroupDefinition.PATH);
        subsystem.rejectChildResource(PathElement.pathElement(REMOTE_CONNECTOR));
        subsystem.rejectChildResource(MessagingExtension.HTTP_CONNECTOR_PATH);
        subsystem.rejectChildResource(PathElement.pathElement(CONNECTOR));
        subsystem.rejectChildResource(PathElement.pathElement(IN_VM_CONNECTOR));
        subsystem.rejectChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        subsystem.rejectChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        subsystem.rejectChildResource(EXTERNAL_JMS_QUEUE_PATH);
        subsystem.rejectChildResource(EXTERNAL_JMS_TOPIC_PATH);

        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);
        // WFLY-10165 - journal-jdbc-network-timeout default value is 20 seconds.
        server.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, ServerDefinition.JOURNAL_JDBC_NETWORK_TIMEOUT);

        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.JOURNAL_JDBC_LOCK_EXPIRATION,
                ServerDefinition.JOURNAL_JDBC_LOCK_RENEW_PERIOD,
                ServerDefinition.JOURNAL_NODE_MANAGER_STORE_TABLE);

        ResourceTransformationDescriptionBuilder addressSetting = server.addChildResource(MessagingExtension.ADDRESS_SETTING_PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting,
                AddressSettingDefinition.AUTO_CREATE_QUEUES,
                AddressSettingDefinition.AUTO_DELETE_QUEUES,
                AddressSettingDefinition.AUTO_CREATE_ADDRESSES,
                AddressSettingDefinition.AUTO_DELETE_ADDRESSES);

        ResourceTransformationDescriptionBuilder connectionFactory = server.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(connectionFactory, ConnectionFactoryAttributes.Common.INITIAL_MESSAGE_PACKET_SIZE);
    }

    private static void registerTransformers_EAP_7_1_0(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);

        server.addChildResource(MessagingExtension.BROADCAST_GROUP_PATH).getAttributeBuilder()
                .setDiscard(new JGroupsChannelDiscardAttributeChecker(), BroadcastGroupDefinition.JGROUPS_CHANNEL)
                .addRejectCheck(RejectAttributeChecker.DEFINED, BroadcastGroupDefinition.JGROUPS_CHANNEL)
                .addRename(CommonAttributes.JGROUPS_CLUSTER, CommonAttributes.JGROUPS_CHANNEL.getName())
                .end();
        server.addChildResource(DiscoveryGroupDefinition.PATH).getAttributeBuilder()
                .setDiscard(new JGroupsChannelDiscardAttributeChecker(), DiscoveryGroupDefinition.JGROUPS_CHANNEL)
                .addRejectCheck(RejectAttributeChecker.DEFINED, DiscoveryGroupDefinition.JGROUPS_CHANNEL)
                .addRename(CommonAttributes.JGROUPS_CLUSTER, CommonAttributes.JGROUPS_CHANNEL.getName())
                .end();
    }

    static class JGroupsChannelDiscardAttributeChecker implements DiscardAttributeChecker {

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
            return operation.get(ModelDescriptionConstants.OP).asString().equals(ModelDescriptionConstants.ADD) && discard(attributeValue, operation);
        }

        @Override
        public boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return discard(attributeValue, context.readResource(PathAddress.EMPTY_ADDRESS).getModel());
        }

        private static boolean discard(ModelNode attributeValue, ModelNode model) {
            // Discard if this was a fabricated channel
            return !model.hasDefined(CommonAttributes.JGROUPS_CLUSTER.getName()) || !attributeValue.isDefined() || (model.hasDefined(CommonAttributes.JGROUPS_CHANNEL_FACTORY.getName()) && model.get(CommonAttributes.JGROUPS_CLUSTER.getName()).equals(attributeValue));
        }
    }

    private static void registerTransformers_EAP_7_0_0(ResourceTransformationDescriptionBuilder subsystem) {
        rejectDefinedAttributeWithDefaultValue(subsystem, MessagingSubsystemRootResourceDefinition.GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE,
                MessagingSubsystemRootResourceDefinition.GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE);

        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);
        // reject journal-datasource, journal-bindings-table introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.ELYTRON_DOMAIN,
                ServerDefinition.JOURNAL_DATASOURCE,
                ServerDefinition.JOURNAL_MESSAGES_TABLE,
                ServerDefinition.JOURNAL_BINDINGS_TABLE,
                ServerDefinition.JOURNAL_JMS_BINDINGS_TABLE,
                ServerDefinition.JOURNAL_LARGE_MESSAGES_TABLE,
                ServerDefinition.JOURNAL_PAGE_STORE_TABLE,
                ServerDefinition.JOURNAL_DATABASE,
                ServerDefinition.JOURNAL_JDBC_NETWORK_TIMEOUT
        );
        server.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, ServerDefinition.CREDENTIAL_REFERENCE)
                .addRejectCheck(DEFINED, ServerDefinition.CREDENTIAL_REFERENCE);
        ResourceTransformationDescriptionBuilder replicationMaster = server.addChildResource(MessagingExtension.REPLICATION_MASTER_PATH);
        replicationMaster.getAttributeBuilder()
                // reject if the attribute is undefined as its default value was changed from false to true in EAP 7.1.0
                .addRejectCheck(UNDEFINED, HAAttributes.CHECK_FOR_LIVE_SERVER);
        ResourceTransformationDescriptionBuilder replicationColocated = server.addChildResource(MessagingExtension.REPLICATION_COLOCATED_PATH);
        ResourceTransformationDescriptionBuilder masterForReplicationColocated = replicationColocated.addChildResource(MessagingExtension.CONFIGURATION_MASTER_PATH);
        masterForReplicationColocated.getAttributeBuilder()
                // reject if the attribute is undefined as its default value was changed from false to true in EAP 7.1.0
                .addRejectCheck(UNDEFINED, HAAttributes.CHECK_FOR_LIVE_SERVER);
        ResourceTransformationDescriptionBuilder bridge = server.addChildResource(MessagingExtension.BRIDGE_PATH);
        bridge.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, BridgeDefinition.CREDENTIAL_REFERENCE)
                .addRejectCheck(DEFINED, BridgeDefinition.CREDENTIAL_REFERENCE);
        ResourceTransformationDescriptionBuilder httpConnector = server.addChildResource(MessagingExtension.HTTP_CONNECTOR_PATH);
        // reject server-name introduced in management version 2.0.0 if it is defined
        rejectDefinedAttributeWithDefaultValue(httpConnector, HTTPConnectorDefinition.SERVER_NAME);
        // reject producer-window-size introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(bridge, BridgeDefinition.PRODUCER_WINDOW_SIZE);
        ResourceTransformationDescriptionBuilder jmsBridge = server.addChildResource(MessagingExtension.JMS_BRIDGE_PATH);
        rejectDefinedAttributeWithDefaultValue(jmsBridge, JMSBridgeDefinition.SOURCE_CREDENTIAL_REFERENCE);
        rejectDefinedAttributeWithDefaultValue(jmsBridge, JMSBridgeDefinition.TARGET_CREDENTIAL_REFERENCE);
        ResourceTransformationDescriptionBuilder clusterConnection = server.addChildResource(MessagingExtension.CLUSTER_CONNECTION_PATH);
        // reject producer-window-size introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(clusterConnection, ClusterConnectionDefinition.PRODUCER_WINDOW_SIZE);
        ResourceTransformationDescriptionBuilder connectionFactory = server.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(connectionFactory, ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST);
        connectionFactory.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, CommonAttributes.CALL_FAILOVER_TIMEOUT);
        ResourceTransformationDescriptionBuilder pooledConnectionFactory = server.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        // reject rebalance-connections introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.REBALANCE_CONNECTIONS);
        // reject statistics-enabled introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.STATISTICS_ENABLED);
        // reject max-pool-size whose default value has been changed in  management version 2.0.0
        // reject min-pool-size whose default value has been changed in  management version 2.0.0
        pooledConnectionFactory.getAttributeBuilder().setValueConverter(AttributeConverter.DEFAULT_VALUE, ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE, CommonAttributes.CALL_FAILOVER_TIMEOUT, ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE);
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.CREDENTIAL_REFERENCE,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST,
                ConnectionFactoryAttributes.Pooled.ALLOW_LOCAL_TRANSACTIONS);
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, attrs)
                .addRejectCheck(DEFINED, attrs);
    }
}
