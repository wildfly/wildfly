/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REMOTE_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REMOTE_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.ADDRESS_SETTING_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.CONFIGURATION_MASTER_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.CONFIGURATION_PRIMARY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.CONFIGURATION_SECONDARY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.CONFIGURATION_SLAVE_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.LIVE_ONLY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SERVER_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.REPLICATION_COLOCATED_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.REPLICATION_MASTER_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.REPLICATION_PRIMARY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.REPLICATION_SECONDARY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.REPLICATION_SLAVE_PATH;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_ALLOWLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_BLOCKLIST;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SHARED_STORE_COLOCATED_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SHARED_STORE_MASTER_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SHARED_STORE_PRIMARY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SHARED_STORE_SECONDARY_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SHARED_STORE_SLAVE_PATH;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.messaging.activemq.ha.ScaleDownAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;

/**
 * {@link ExtensionTransformerRegistration} for the messaging-activemq subsystem.
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
        registerTransformers_WF_36(builder.createBuilder(MessagingExtension.VERSION_17_0_0, MessagingExtension.VERSION_16_0_0));
        registerTransformers_WF_29(builder.createBuilder(MessagingExtension.VERSION_16_0_0, MessagingExtension.VERSION_15_0_0));
        registerTransformers_WF_28(builder.createBuilder(MessagingExtension.VERSION_15_0_0, MessagingExtension.VERSION_14_0_0));
        registerTransformers_WF_27(builder.createBuilder(MessagingExtension.VERSION_14_0_0, MessagingExtension.VERSION_13_1_0));
        registerTransformers_WF_26_1(builder.createBuilder(MessagingExtension.VERSION_13_1_0, MessagingExtension.VERSION_13_0_0));
        builder.buildAndRegister(registration, new ModelVersion[]{MessagingExtension.VERSION_13_0_0, MessagingExtension.VERSION_13_1_0,
            MessagingExtension.VERSION_14_0_0, MessagingExtension.VERSION_15_0_0, MessagingExtension.VERSION_16_0_0, MessagingExtension.VERSION_17_0_0});
    }

    private static void registerTransformers_WF_36(ResourceTransformationDescriptionBuilder subsystem) {
        rejectDefinedAttributeWithDefaultValue(subsystem.addChildResource(SERVER_PATH).addChildResource(LIVE_ONLY_PATH), ScaleDownAttributes.SCALE_DOWN_COMMIT_INTERVAL);
        rejectDefinedAttributeWithDefaultValue(subsystem.addChildResource(SERVER_PATH).addChildResource(REPLICATION_SECONDARY_PATH), ScaleDownAttributes.SCALE_DOWN_COMMIT_INTERVAL);
        rejectDefinedAttributeWithDefaultValue(subsystem.addChildResource(SERVER_PATH).addChildResource(SHARED_STORE_SECONDARY_PATH), ScaleDownAttributes.SCALE_DOWN_COMMIT_INTERVAL);
    }

    private static void registerTransformers_WF_29(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder addressSettings = subsystem.addChildResource(SERVER_PATH)
                .addChildResource(ADDRESS_SETTING_PATH);
        rejectDefinedAttributeWithDefaultValue(addressSettings, AddressSettingDefinition.MAX_READ_PAGE_BYTES);
    }

    private static void registerTransformers_WF_28(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(SERVER_PATH);
        server.addChildResource(PathElement.pathElement(REMOTE_ACCEPTOR)).getAttributeBuilder()
                .addRejectCheck(DEFINED, CommonAttributes.SSL_CONTEXT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CommonAttributes.SSL_CONTEXT);
        server.addChildResource(PathElement.pathElement(HTTP_ACCEPTOR)).getAttributeBuilder()
                .addRejectCheck(DEFINED, CommonAttributes.SSL_CONTEXT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CommonAttributes.SSL_CONTEXT);
        server.addChildResource(PathElement.pathElement(REMOTE_CONNECTOR)).getAttributeBuilder()
                .addRejectCheck(DEFINED, CommonAttributes.SSL_CONTEXT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CommonAttributes.SSL_CONTEXT);
        server.addChildResource(PathElement.pathElement(HTTP_CONNECTOR)).getAttributeBuilder()
                .addRejectCheck(DEFINED, CommonAttributes.SSL_CONTEXT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CommonAttributes.SSL_CONTEXT);
    }

    private static void registerTransformers_WF_27(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder externaljmsqueue = subsystem.addChildResource(MessagingExtension.EXTERNAL_JMS_QUEUE_PATH);
        rejectDefinedAttributeWithDefaultValue(externaljmsqueue, ConnectionFactoryAttributes.External.ENABLE_AMQ1_PREFIX);
        ResourceTransformationDescriptionBuilder externaljmstopic = subsystem.addChildResource(MessagingExtension.EXTERNAL_JMS_TOPIC_PATH);
        rejectDefinedAttributeWithDefaultValue(externaljmstopic, ConnectionFactoryAttributes.External.ENABLE_AMQ1_PREFIX);

        ResourceTransformationDescriptionBuilder externalConnectionFactory = subsystem.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        renameAttribute(externalConnectionFactory, DESERIALIZATION_BLACKLIST, DESERIALIZATION_BLOCKLIST);
        renameAttribute(externalConnectionFactory, DESERIALIZATION_WHITELIST, DESERIALIZATION_ALLOWLIST);

        ResourceTransformationDescriptionBuilder pooledExternalConnectionFactory = subsystem.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        renameAttribute(pooledExternalConnectionFactory, DESERIALIZATION_BLACKLIST, DESERIALIZATION_BLOCKLIST);
        renameAttribute(pooledExternalConnectionFactory, DESERIALIZATION_WHITELIST, DESERIALIZATION_ALLOWLIST);

        ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(MessagingExtension.SERVER_PATH);
        ResourceTransformationDescriptionBuilder bridge = server.addChildResource(MessagingExtension.BRIDGE_PATH);
        rejectDefinedAttributeWithDefaultValue(bridge, BridgeDefinition.ROUTING_TYPE);

        ResourceTransformationDescriptionBuilder connectionFactory = server.addChildResource(MessagingExtension.CONNECTION_FACTORY_PATH);
        renameAttribute(connectionFactory, DESERIALIZATION_BLACKLIST, DESERIALIZATION_BLOCKLIST);
        renameAttribute(connectionFactory, DESERIALIZATION_WHITELIST, DESERIALIZATION_ALLOWLIST);

        ResourceTransformationDescriptionBuilder pooledConnectionFactory = server.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        renameAttribute(pooledConnectionFactory, DESERIALIZATION_BLACKLIST, DESERIALIZATION_BLOCKLIST);
        renameAttribute(pooledConnectionFactory, DESERIALIZATION_WHITELIST, DESERIALIZATION_ALLOWLIST);

        server.addChildRedirection(REPLICATION_PRIMARY_PATH, REPLICATION_MASTER_PATH);
        server.addChildRedirection(REPLICATION_SECONDARY_PATH, REPLICATION_SLAVE_PATH);
        server.addChildRedirection(SHARED_STORE_PRIMARY_PATH, SHARED_STORE_MASTER_PATH);
        server.addChildRedirection(SHARED_STORE_SECONDARY_PATH, SHARED_STORE_SLAVE_PATH);

        ResourceTransformationDescriptionBuilder colocatedSharedStore = server.addChildResource(SHARED_STORE_COLOCATED_PATH);
        colocatedSharedStore.addChildRedirection(CONFIGURATION_PRIMARY_PATH, CONFIGURATION_MASTER_PATH);
        colocatedSharedStore.addChildRedirection(CONFIGURATION_SECONDARY_PATH, CONFIGURATION_SLAVE_PATH);
        ResourceTransformationDescriptionBuilder colocatedReplication = server.addChildResource(REPLICATION_COLOCATED_PATH);
        colocatedReplication.addChildRedirection(CONFIGURATION_PRIMARY_PATH, CONFIGURATION_MASTER_PATH);
        colocatedReplication.addChildRedirection(CONFIGURATION_SECONDARY_PATH, CONFIGURATION_SLAVE_PATH);

        ResourceTransformationDescriptionBuilder addressSetting = server.addChildResource(MessagingExtension.ADDRESS_SETTING_PATH);
        rejectDefinedAttributeWithDefaultValue(addressSetting,
                AddressSettingDefinition.AUTO_DELETE_CREATED_QUEUES);
    }

    private static void registerTransformers_WF_26_1(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder server = subsystem
                .addChildResource(MessagingExtension.SERVER_PATH);
        rejectDefinedAttributeWithDefaultValue(server, ServerDefinition.ADDRESS_QUEUE_SCAN_PERIOD);
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, attrs)
                .addRejectCheck(DEFINED, attrs);
    }

    private static void renameAttribute(ResourceTransformationDescriptionBuilder resourceRegistry, AttributeDefinition attribute, AttributeDefinition newAttribute) {
        resourceRegistry.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, newAttribute)
                .addRename(newAttribute, attribute.getName());
    }
}
