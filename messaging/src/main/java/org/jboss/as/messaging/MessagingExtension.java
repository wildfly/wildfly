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

package org.jboss.as.messaging;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import org.jboss.as.messaging.jms.ConnectionFactoryAdd;
import org.jboss.as.messaging.jms.ConnectionFactoryRemove;
import org.jboss.as.messaging.jms.ConnectionFactoryWriteAttributeHandler;
import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSQueueRemove;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.as.messaging.jms.JMSTopicRemove;
import org.jboss.as.messaging.jms.JmsQueueConfigurationWriteHandler;
import org.jboss.as.messaging.jms.PooledConnectionFactoryAdd;
import org.jboss.as.messaging.jms.PooledConnectionFactoryRemove;
import org.jboss.as.messaging.jms.PooledConnectionFactoryWriteAttributeHandler;
import org.jboss.as.messaging.jms.TopicConfigurationWriteHandler;

/**
 * Domain extension that integrates HornetQ.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    private static final PathElement SUBSYSTEM_PATH  = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final PathElement ADDRESS_SETTING = PathElement.pathElement(CommonAttributes.ADDRESS_SETTING);

    static final PathElement GENERIC_ACCEPTOR = PathElement.pathElement(CommonAttributes.ACCEPTOR);
    static final PathElement REMOTE_ACCEPTOR = PathElement.pathElement(CommonAttributes.REMOTE_ACCEPTOR);
    static final PathElement IN_VM_ACCEPTOR = PathElement.pathElement(CommonAttributes.IN_VM_ACCEPTOR);

    static final PathElement GENERIC_CONNECTOR = PathElement.pathElement(CommonAttributes.CONNECTOR);
    static final PathElement REMOTE_CONNECTOR = PathElement.pathElement(CommonAttributes.REMOTE_CONNECTOR);
    static final PathElement IN_VM_CONNECTOR = PathElement.pathElement(CommonAttributes.IN_VM_CONNECTOR);

    static final PathElement PARAM = PathElement.pathElement(CommonAttributes.PARAM);

    private static final PathElement CFS_PATH = PathElement.pathElement(CommonAttributes.CONNECTION_FACTORY);
    private static final PathElement JMS_QUEUE_PATH = PathElement.pathElement(CommonAttributes.JMS_QUEUE);
    private static final PathElement TOPIC_PATH = PathElement.pathElement(CommonAttributes.JMS_TOPIC);
    private static final PathElement RA_PATH = PathElement.pathElement(CommonAttributes.POOLED_CONNECTION_FACTORY);
    private static final PathElement BROADCAST_GROUP_PATH = PathElement.pathElement(CommonAttributes.BROADCAST_GROUP);
    private static final PathElement DISCOVERY_GROUP_PATH = PathElement.pathElement(CommonAttributes.DISCOVERY_GROUP);
    private static final PathElement DIVERT_PATH = PathElement.pathElement(CommonAttributes.DIVERT);
    private static final PathElement GROUPING_HANDLER_PATH = PathElement.pathElement(CommonAttributes.GROUPING_HANDLER);

    static final PathElement SECURITY_ROLE = PathElement.pathElement(CommonAttributes.ROLE);
    static final PathElement SECURITY_SETTING = PathElement.pathElement(CommonAttributes.SECURITY_SETTING);

    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        subsystem.registerXMLElementWriter(MessagingSubsystemParser.getInstance());

        final ManagementResourceRegistration rootRegistration = subsystem.registerSubsystemModel(MessagingSubsystemProviders.SUBSYSTEM);

        rootRegistration.registerOperationHandler(ADD, MessagingSubsystemAdd.INSTANCE, MessagingSubsystemAdd.INSTANCE, false);
        rootRegistration.registerOperationHandler(REMOVE, MessagingSubsystemRemove.INSTANCE, MessagingSubsystemRemove.INSTANCE);
        // TODO REMOVE op
        rootRegistration.registerOperationHandler(DESCRIBE, MessagingSubsystemDescribeHandler.INSTANCE, MessagingSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        for (AttributeDefinition attributeDefinition : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            rootRegistration.registerReadWriteAttribute(attributeDefinition.getName(), null, HornetQServerControlWriteHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Address settings
        final ManagementResourceRegistration address = rootRegistration.registerSubModel(ADDRESS_SETTING, MessagingSubsystemProviders.ADDRESS_SETTING);
        address.registerOperationHandler(ADD, AddressSettingAdd.INSTANCE, MessagingSubsystemProviders.ADDRESS_SETTING_ADD);
        address.registerOperationHandler(REMOVE, AddressSettingRemove.INSTANCE, MessagingSubsystemProviders.ADDRESS_SETTING_REMOVE);
        for(final AttributeDefinition definition : AddressSettingAdd.ATTRIBUTES) {
            address.registerReadWriteAttribute(definition.getName(), null, AddressSettingsWriteHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Broadcast groups
        final ManagementResourceRegistration broadcastGroups = rootRegistration.registerSubModel(BROADCAST_GROUP_PATH, MessagingSubsystemProviders.BROADCAST_GROUP_RESOURCE);
        broadcastGroups.registerOperationHandler(ADD, BroadcastGroupAdd.INSTANCE, BroadcastGroupAdd.INSTANCE);
        broadcastGroups.registerOperationHandler(REMOVE, BroadcastGroupRemove.INSTANCE, BroadcastGroupRemove.INSTANCE);
        for (AttributeDefinition attributeDefinition : CommonAttributes.BROADCAST_GROUP_ATTRIBUTES) {
            broadcastGroups.registerReadWriteAttribute(attributeDefinition.getName(), null, BroadcastGroupWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }
        // TODO operations exposed by BroadcastGroupControl
        // Discovery groups
        final ManagementResourceRegistration discoveryGroups = rootRegistration.registerSubModel(DISCOVERY_GROUP_PATH, MessagingSubsystemProviders.DISCOVERY_GROUP_RESOURCE);
        discoveryGroups.registerOperationHandler(ADD, DiscoveryGroupAdd.INSTANCE, DiscoveryGroupAdd.INSTANCE);
        discoveryGroups.registerOperationHandler(REMOVE, DiscoveryGroupRemove.INSTANCE, DiscoveryGroupRemove.INSTANCE);
        for (AttributeDefinition attributeDefinition : CommonAttributes.DISCOVERY_GROUP_ATTRIBUTES) {
            discoveryGroups.registerReadWriteAttribute(attributeDefinition.getName(), null, DiscoveryGroupWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Diverts
        final ManagementResourceRegistration diverts = rootRegistration.registerSubModel(DIVERT_PATH, MessagingSubsystemProviders.DIVERT_RESOURCE);
        diverts.registerOperationHandler(ADD, DivertAdd.INSTANCE, DivertAdd.INSTANCE);
        diverts.registerOperationHandler(REMOVE, DivertRemove.INSTANCE, DivertRemove.INSTANCE);
        for (AttributeDefinition attributeDefinition : CommonAttributes.DIVERT_ATTRIBUTES) {
            diverts.registerReadWriteAttribute(attributeDefinition.getName(), null, DivertConfigurationWriteHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Core queues
        final ManagementResourceRegistration queue = rootRegistration.registerSubModel(PathElement.pathElement(QUEUE), MessagingSubsystemProviders.QUEUE_RESOURCE);
        queue.registerOperationHandler(ADD, QueueAdd.INSTANCE, QueueAdd.INSTANCE, false);
        queue.registerOperationHandler(REMOVE, QueueRemove.INSTANCE, QueueRemove.INSTANCE, false);
        for (AttributeDefinition attributeDefinition : CommonAttributes.CORE_QUEUE_ATTRIBUTES) {
            queue.registerReadWriteAttribute(attributeDefinition.getName(), null, QueueConfigurationWriteHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        final ManagementResourceRegistration acceptor = rootRegistration.registerSubModel(GENERIC_ACCEPTOR, MessagingSubsystemProviders.ACCEPTOR);
        acceptor.registerOperationHandler(ADD, TransportConfigOperationHandlers.GENERIC_ADD, MessagingSubsystemProviders.ACCEPTOR_ADD);
        acceptor.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.ACCEPTOR_REMOVE);
        for(final AttributeDefinition def : TransportConfigOperationHandlers.GENERIC) {
            acceptor.registerReadWriteAttribute(def.getName(), null, TransportConfigOperationHandlers.GENERIC_ATTR, AttributeAccess.Storage.CONFIGURATION);
        }
        createParamRegistration(acceptor);

        // remote acceptor
        final ManagementResourceRegistration remoteAcceptor = rootRegistration.registerSubModel(REMOTE_ACCEPTOR, MessagingSubsystemProviders.REMOTE_ACCEPTOR);
        remoteAcceptor.registerOperationHandler(ADD, TransportConfigOperationHandlers.REMOTE_ADD, MessagingSubsystemProviders.REMOTE_ACCEPTOR_ADD);
        remoteAcceptor.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.ACCEPTOR_REMOVE);
        for(final AttributeDefinition def : TransportConfigOperationHandlers.REMOTE) {
            remoteAcceptor.registerReadWriteAttribute(def.getName(), null, TransportConfigOperationHandlers.REMOTE_ATTR, AttributeAccess.Storage.CONFIGURATION);
        }
        createParamRegistration(remoteAcceptor);

        // in-vm acceptor
        final ManagementResourceRegistration inVMAcceptor = rootRegistration.registerSubModel(IN_VM_ACCEPTOR, MessagingSubsystemProviders.IN_VM_ACCEPTOR);
        inVMAcceptor.registerOperationHandler(ADD, TransportConfigOperationHandlers.IN_VM_ADD, MessagingSubsystemProviders.IN_VM_ACCEPTOR_ADD);
        inVMAcceptor.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.ACCEPTOR_REMOVE);
        for(final AttributeDefinition def : TransportConfigOperationHandlers.IN_VM) {
            inVMAcceptor.registerReadWriteAttribute(def.getName(), null, TransportConfigOperationHandlers.IN_VM_ATTR, AttributeAccess.Storage.CONFIGURATION);
        }
        createParamRegistration(inVMAcceptor);

        // connector
        final ManagementResourceRegistration connector = rootRegistration.registerSubModel(GENERIC_CONNECTOR, MessagingSubsystemProviders.CONNECTOR);
        connector.registerOperationHandler(ADD, TransportConfigOperationHandlers.GENERIC_ADD, MessagingSubsystemProviders.CONNECTOR_ADD);
        connector.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.CONNECTOR_REMOVE);
        for(final AttributeDefinition def : TransportConfigOperationHandlers.GENERIC) {
            connector.registerReadWriteAttribute(def.getName(), null, TransportConfigOperationHandlers.GENERIC_ATTR, AttributeAccess.Storage.CONFIGURATION);
        }
        createParamRegistration(connector);

        // remote connector
        final ManagementResourceRegistration remoteConnector = rootRegistration.registerSubModel(REMOTE_CONNECTOR, MessagingSubsystemProviders.REMOTE_CONNECTOR);
        remoteConnector.registerOperationHandler(ADD, TransportConfigOperationHandlers.REMOTE_ADD, MessagingSubsystemProviders.REMOTE_CONNECTOR_ADD);
        remoteConnector.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.CONNECTOR_REMOVE);
        for(final AttributeDefinition def : TransportConfigOperationHandlers.REMOTE) {
            remoteConnector.registerReadWriteAttribute(def.getName(), null, TransportConfigOperationHandlers.REMOTE_ATTR, AttributeAccess.Storage.CONFIGURATION);
        }
        createParamRegistration(remoteConnector);

        // in-vm connector
        final ManagementResourceRegistration inVMConnector = rootRegistration.registerSubModel(IN_VM_CONNECTOR, MessagingSubsystemProviders.IN_VM_CONNECTOR);
        inVMConnector.registerOperationHandler(ADD, TransportConfigOperationHandlers.IN_VM_ADD, MessagingSubsystemProviders.IN_VM_CONNECTOR_ADD);
        inVMConnector.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.CONNECTOR_REMOVE);
        for(final AttributeDefinition def : TransportConfigOperationHandlers.IN_VM) {
            inVMConnector.registerReadWriteAttribute(def.getName(), null, TransportConfigOperationHandlers.IN_VM_ATTR, AttributeAccess.Storage.CONFIGURATION);
        }
        createParamRegistration(inVMConnector);

        // Bridges
        final ManagementResourceRegistration bridge = rootRegistration.registerSubModel(PathElement.pathElement(CommonAttributes.BRIDGE), MessagingSubsystemProviders.BRIDGE_RESOURCE);
        bridge.registerOperationHandler(ADD, BridgeAdd.INSTANCE, BridgeAdd.INSTANCE, false);
        bridge.registerOperationHandler(REMOVE, BridgeRemove.INSTANCE, BridgeRemove.INSTANCE, false);
        for (AttributeDefinition attributeDefinition : CommonAttributes.BRIDGE_ATTRIBUTES) {
            bridge.registerReadWriteAttribute(attributeDefinition.getName(), null, BridgeWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Cluster connections
        final ManagementResourceRegistration cluster = rootRegistration.registerSubModel(PathElement.pathElement(CommonAttributes.CLUSTER_CONNECTION),
                MessagingSubsystemProviders.CLUSTER_CONNECTION_RESOURCE);
        cluster.registerOperationHandler(ADD, ClusterConnectionAdd.INSTANCE, ClusterConnectionAdd.INSTANCE, false);
        cluster.registerOperationHandler(REMOVE, ClusterConnectionRemove.INSTANCE, ClusterConnectionRemove.INSTANCE, false);
        for (AttributeDefinition attributeDefinition : CommonAttributes.CLUSTER_CONNECTION_ATTRIBUTES) {
            cluster.registerReadWriteAttribute(attributeDefinition.getName(), null, ClusterConnectionWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Grouping Handler
        final ManagementResourceRegistration groupingHandler = rootRegistration.registerSubModel(GROUPING_HANDLER_PATH, MessagingSubsystemProviders.GROUPING_HANDLER_RESOURCE);
        groupingHandler.registerOperationHandler(ADD, GroupingHandlerAdd.INSTANCE, GroupingHandlerAdd.INSTANCE);
        groupingHandler.registerOperationHandler(REMOVE, GroupingHandlerRemove.INSTANCE, GroupingHandlerRemove.INSTANCE);
        for (AttributeDefinition attributeDefinition : CommonAttributes.GROUPING_HANDLER_ATTRIBUTES) {
            groupingHandler.registerReadWriteAttribute(attributeDefinition.getName(), null, GroupingHandlerWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Connector services
        final ManagementResourceRegistration connectorService = rootRegistration.registerSubModel(PathElement.pathElement(CommonAttributes.CONNECTOR_SERVICE),
                MessagingSubsystemProviders.CONNECTOR_SERVICE_RESOURCE);
        connectorService.registerOperationHandler(ADD, ConnectorServiceAdd.INSTANCE, ConnectorServiceAdd.INSTANCE, false);
        connectorService.registerOperationHandler(REMOVE, ConnectorServiceRemove.INSTANCE, ConnectorServiceRemove.INSTANCE, false);
        for (AttributeDefinition attributeDefinition : CommonAttributes.CONNECTOR_SERVICE_ATTRIBUTES) {
            connectorService.registerReadWriteAttribute(attributeDefinition.getName(), null, ConnectorServiceWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }
        final ManagementResourceRegistration connectorServiceParam = connectorService.registerSubModel(PathElement.pathElement(CommonAttributes.PARAM),
                MessagingSubsystemProviders.CONNECTOR_SERVICE_PARAM_RESOURCE);
        connectorServiceParam.registerOperationHandler(ADD, ConnectorServiceParamAdd.INSTANCE, ConnectorServiceParamAdd.INSTANCE, false);
        connectorServiceParam.registerOperationHandler(REMOVE, ConnectorServiceParamRemove.INSTANCE, ConnectorServiceParamRemove.INSTANCE, false);
        connectorServiceParam.registerReadWriteAttribute(CommonAttributes.VALUE.getName(), null, ConnectorServiceParamWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

        // Messaging paths
        for(final String path : CommonAttributes.PATHS) {
            ManagementResourceRegistration bindings = rootRegistration.registerSubModel(PathElement.pathElement(PATH, path), MessagingSubsystemProviders.PATH);
            MessagingPathHandlers.register(bindings);
        }

        // Connection factories
        final ManagementResourceRegistration cfs = rootRegistration.registerSubModel(CFS_PATH, MessagingSubsystemProviders.CF);
        cfs.registerOperationHandler(ADD, ConnectionFactoryAdd.INSTANCE, MessagingSubsystemProviders.CF_ADD, false);
        cfs.registerOperationHandler(REMOVE, ConnectionFactoryRemove.INSTANCE, MessagingSubsystemProviders.CF_REMOVE, false);
        for (AttributeDefinition attributeDefinition : JMSServices.CONNECTION_FACTORY_ATTRS) {
            cfs.registerReadWriteAttribute(attributeDefinition.getName(), null, ConnectionFactoryWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // Resource Adapter Pooled connection factories
        final ManagementResourceRegistration resourceAdapters = rootRegistration.registerSubModel(RA_PATH, MessagingSubsystemProviders.RA);
        resourceAdapters.registerOperationHandler(ADD, PooledConnectionFactoryAdd.INSTANCE, MessagingSubsystemProviders.RA_ADD, false);
        resourceAdapters.registerOperationHandler(REMOVE, PooledConnectionFactoryRemove.INSTANCE, MessagingSubsystemProviders.RA_REMOVE);
        for (AttributeDefinition attributeDefinition : JMSServices.POOLED_CONNECTION_FACTORY_ATTRS) {
            resourceAdapters.registerReadWriteAttribute(attributeDefinition.getName(), null, PooledConnectionFactoryWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }

        // JMS Queues
        final ManagementResourceRegistration queues = rootRegistration.registerSubModel(JMS_QUEUE_PATH, MessagingSubsystemProviders.JMS_QUEUE_RESOURCE);
        queues.registerOperationHandler(ADD, JMSQueueAdd.INSTANCE, JMSQueueAdd.INSTANCE, false);
        queues.registerOperationHandler(REMOVE, JMSQueueRemove.INSTANCE, JMSQueueRemove.INSTANCE, false);
        for (AttributeDefinition attributeDefinition : CommonAttributes.JMS_QUEUE_ATTRIBUTES) {
            queues.registerReadWriteAttribute(attributeDefinition.getName(), null, JmsQueueConfigurationWriteHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }
        // TODO runtime operations exposed by JmsQueueControl

        // JMS Topics
        final ManagementResourceRegistration topics = rootRegistration.registerSubModel(TOPIC_PATH, MessagingSubsystemProviders.JMS_TOPIC_RESOURCE);
        topics.registerOperationHandler(ADD, JMSTopicAdd.INSTANCE, JMSTopicAdd.INSTANCE, false);
        topics.registerOperationHandler(REMOVE, JMSTopicRemove.INSTANCE, JMSTopicRemove.INSTANCE, false);
        topics.registerReadWriteAttribute(CommonAttributes.ENTRIES.getName(), null, TopicConfigurationWriteHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        // TODO runtime operations exposed by TopicControl


        final ManagementResourceRegistration securitySettings = rootRegistration.registerSubModel(SECURITY_SETTING, MessagingSubsystemProviders.SECURITY_SETTING);
        securitySettings.registerOperationHandler(ADD, SecuritySettingAdd.INSTANCE, SecuritySettingAdd.INSTANCE);
        securitySettings.registerOperationHandler(REMOVE, SecuritySettingRemove.INSTANCE, SecuritySettingRemove.INSTANCE);

        final ManagementResourceRegistration securityRole = securitySettings.registerSubModel(SECURITY_ROLE, MessagingSubsystemProviders.SECURITY_ROLE);
        securityRole.registerOperationHandler(ADD, SecurityRoleAdd.INSTANCE, SecurityRoleAdd.INSTANCE);
        securityRole.registerOperationHandler(REMOVE, SecurityRoleAdd.INSTANCE, SecurityRoleAdd.INSTANCE);
        for(final AttributeDefinition def : SecurityRoleAdd.ROLE_ATTRIBUTES) {
            securityRole.registerReadWriteAttribute(def.getName(), null, SecurityRoleAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        }
    }

    public void initializeParsers(ExtensionParsingContext context) {
        for(Namespace namespace : Namespace.values()) {
            context.setSubsystemXmlMapping(namespace.getUriString(), MessagingSubsystemParser.getInstance());
        }
    }

    static void createParamRegistration(final ManagementResourceRegistration parent) {
        final ManagementResourceRegistration registration = parent.registerSubModel(PARAM, MessagingSubsystemProviders.PARAM);
        registration.registerOperationHandler(ADD, TransportConfigOperationHandlers.PARAM_ADD, MessagingSubsystemProviders.PARAM_ADD);
        registration.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.PARAM_REMOVE);
        registration.registerReadWriteAttribute("value", null, TransportConfigOperationHandlers.PARAM_ATTR, AttributeAccess.Storage.CONFIGURATION);
    }

}
