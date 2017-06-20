/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.UNDEFINED;
import static org.jboss.dmr.ModelType.INT;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.ha.HAAttributes;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;
import org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the messaging subsystem root resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingSubsystemRootResourceDefinition extends PersistentResourceDefinition {

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE = create("global-client-thread-pool-max-size", INT)
            .setAttributeGroup("global-client")
            .setXmlName("thread-pool-max-size")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE = create("global-client-scheduled-thread-pool-max-size", INT)
            .setAttributeGroup("global-client")
            .setXmlName("scheduled-thread-pool-max-size")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
            GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE,
            GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE
    };

    public static final MessagingSubsystemRootResourceDefinition INSTANCE = new MessagingSubsystemRootResourceDefinition();

    private MessagingSubsystemRootResourceDefinition() {
        super(MessagingExtension.SUBSYSTEM_PATH,
                MessagingExtension.getResourceDescriptionResolver(MessagingExtension.SUBSYSTEM_NAME),
                MessagingSubsystemAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler() {
                    @Override
                    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
                        super.performRuntime(context, operation, model);
                        context.removeService(MessagingServices.ACTIVEMQ_CLIENT_THREAD_POOL);
                    }
                });
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    public static void registerTransformers(SubsystemRegistration subsystemRegistration) {
        registerTransformers_EAP_7_0_0(subsystemRegistration);
    }

    private static void registerTransformers_EAP_7_0_0(SubsystemRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder subsystem = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

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
        ResourceTransformationDescriptionBuilder pooledConnectionFactory = server.addChildResource(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH);
        // reject rebalance-connections introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.REBALANCE_CONNECTIONS);
        // reject statistics-enabled introduced in management version 2.0.0 if it is defined and different from the default value.
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.STATISTICS_ENABLED);
        // reject max-pool-size whose default value has been changed in  management version 2.0.0
        defaultValueAttributeConverter(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE);
        // reject min-pool-size whose default value has been changed in  management version 2.0.0
        defaultValueAttributeConverter(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE);
        rejectDefinedAttributeWithDefaultValue(pooledConnectionFactory, ConnectionFactoryAttributes.Pooled.CREDENTIAL_REFERENCE,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_BLACKLIST,
                ConnectionFactoryAttributes.Common.DESERIALIZATION_WHITELIST);

        TransformationDescription.Tools.register(subsystem.build(), subsystemRegistration, MessagingExtension.VERSION_1_0_0);
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

    /**
     * Reject the attributes if they are defined.
     */
    private static void rejectDefinedAttribute(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        for (AttributeDefinition attr : attrs) {
            builder.getAttributeBuilder()
                    .addRejectCheck(DEFINED, attr);
        }
    }

    private static void defaultValueAttributeConverter(ResourceTransformationDescriptionBuilder builder, AttributeDefinition attr) {
       builder.getAttributeBuilder()
                .setValueConverter(new DefaultValueAttributeConverter(attr), attr);
    }
}
