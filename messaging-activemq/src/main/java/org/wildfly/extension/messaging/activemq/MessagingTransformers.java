/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.UNDEFINED;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.VERSION_1_0_0;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.VERSION_2_0_0;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.VERSION_3_0_0;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.ha.HAAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;
import org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class MessagingTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return MessagingExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(VERSION_3_0_0);

        // 3.0.0 -> 2.0.0 (WildFly 11.0.0.Final)
        buildTransformers2_0_0(chainedBuilder.createBuilder(VERSION_3_0_0, VERSION_2_0_0));
        // 2.0.0 -> 1.0.0 (EAP 7.0.0)
        buildTransformers1_0_0(chainedBuilder.createBuilder(VERSION_2_0_0, VERSION_1_0_0));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                VERSION_1_0_0,
                VERSION_2_0_0,
                VERSION_3_0_0
        });
    }

    private static void buildTransformers1_0_0(ResourceTransformationDescriptionBuilder subsystem) {
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
        // if the cluster-connection-address is not defined or equals to "", we transform its value to "jms" for legacy versions
        clusterConnection.getAttributeBuilder()
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                    @Override
                    protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        if (!attributeValue.isDefined() || attributeValue.asString().equals("")) {
                            attributeValue.set("jms");
                        }
                    }
                }, ClusterConnectionDefinition.ADDRESS);
        ResourceTransformationDescriptionBuilder connectionFactory = server.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        rejectDefinedAttributeWithDefaultValue(connectionFactory, ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST);
        defaultValueAttributeConverter(connectionFactory, CommonAttributes.CALL_FAILOVER_TIMEOUT);
        ResourceTransformationDescriptionBuilder pooledConnectionFactory = server.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        // reject rebalance-connections introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.REBALANCE_CONNECTIONS);
        // reject statistics-enabled introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.STATISTICS_ENABLED);
        // reject max-pool-size whose default value has been changed in  management version 2.0.0
        defaultValueAttributeConverter(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE);
        defaultValueAttributeConverter(pooledConnectionFactory, CommonAttributes.CALL_FAILOVER_TIMEOUT);
        // reject min-pool-size whose default value has been changed in  management version 2.0.0
        defaultValueAttributeConverter(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE);
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.CREDENTIAL_REFERENCE,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST,
                ConnectionFactoryAttributes.Pooled.ALLOW_LOCAL_TRANSACTIONS);
    }

    private static void buildTransformers2_0_0(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);

        rejectDefinedAttributeWithDefaultValue(server,
                ServerDefinition.JOURNAL_JDBC_LOCK_ACQUISITION_TIMEOUT,
                ServerDefinition.JOURNAL_JDBC_LOCK_EXPIRATION,
                ServerDefinition.JOURNAL_JDBC_LOCK_RENEW_PERIOD,
                ServerDefinition.JOURNAL_NODE_MANAGER_STORE_TABLE
        );
        ResourceTransformationDescriptionBuilder connectionFactory = server.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        // initial-message-packet-size is now a configuration property of the connection-factory (was a runtime attribute before)
        rejectDefinedAttributeWithDefaultValue(connectionFactory, ConnectionFactoryAttributes.Common.INITIAL_MESSAGE_PACKET_SIZE);
    }

    private static void defaultValueAttributeConverter(ResourceTransformationDescriptionBuilder builder, AttributeDefinition attr) {
        builder.getAttributeBuilder()
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(attr), attr);
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(attr.getDefaultValue()), attr)
                    .addRejectCheck(DEFINED, attr);
        }
    }
}