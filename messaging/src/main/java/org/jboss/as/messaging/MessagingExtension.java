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

import java.util.EnumSet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.messaging.jms.ConnectionFactoryAdd;
import org.jboss.as.messaging.jms.ConnectionFactoryAddJndiHandler;
import org.jboss.as.messaging.jms.ConnectionFactoryReadAttributeHandler;
import org.jboss.as.messaging.jms.ConnectionFactoryRemove;
import org.jboss.as.messaging.jms.ConnectionFactoryWriteAttributeHandler;
import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSQueueAddJndiHandler;
import org.jboss.as.messaging.jms.JMSQueueControlHandler;
import org.jboss.as.messaging.jms.JMSQueueRemove;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.as.messaging.jms.JMSTopicAddJndiHandler;
import org.jboss.as.messaging.jms.JMSTopicConfigurationWriteHandler;
import org.jboss.as.messaging.jms.JMSTopicControlHandler;
import org.jboss.as.messaging.jms.JMSTopicReadAttributeHandler;
import org.jboss.as.messaging.jms.JMSTopicRemove;
import org.jboss.as.messaging.jms.JmsQueueConfigurationWriteHandler;
import org.jboss.as.messaging.jms.JmsQueueReadAttributeHandler;
import org.jboss.as.messaging.jms.PooledConnectionFactoryAdd;
import org.jboss.as.messaging.jms.PooledConnectionFactoryRemove;
import org.jboss.as.messaging.jms.PooledConnectionFactoryWriteAttributeHandler;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;

/**
 * Domain extension that integrates HornetQ.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    private static final String RESOURCE_NAME = MessagingDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    static final PathElement ADDRESS_SETTING = PathElement.pathElement(CommonAttributes.ADDRESS_SETTING);

    static final PathElement GENERIC_ACCEPTOR = PathElement.pathElement(CommonAttributes.ACCEPTOR);
    static final PathElement REMOTE_ACCEPTOR = PathElement.pathElement(CommonAttributes.REMOTE_ACCEPTOR);
    static final PathElement IN_VM_ACCEPTOR = PathElement.pathElement(CommonAttributes.IN_VM_ACCEPTOR);

    static final PathElement GENERIC_CONNECTOR = PathElement.pathElement(CommonAttributes.CONNECTOR);
    static final PathElement REMOTE_CONNECTOR = PathElement.pathElement(CommonAttributes.REMOTE_CONNECTOR);
    static final PathElement IN_VM_CONNECTOR = PathElement.pathElement(CommonAttributes.IN_VM_CONNECTOR);

    static final PathElement PARAM = PathElement.pathElement(CommonAttributes.PARAM);

    private static final PathElement CORE_ADDRESS_PATH = PathElement.pathElement(CommonAttributes.CORE_ADDRESS);
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

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, true);
    }

    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        subsystem.registerXMLElementWriter(MessagingSubsystemParser.getInstance());

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Root resource
        final ManagementResourceRegistration rootRegistration = subsystem.registerSubsystemModel(MessagingSubsystemRootResourceDefinition.INSTANCE);
        rootRegistration.registerOperationHandler(DESCRIBE, MessagingSubsystemDescribeHandler.INSTANCE, MessagingSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        // HQ servers
        final ManagementResourceRegistration serverRegistration = rootRegistration.registerSubModel(new HornetQServerResourceDefinition(registerRuntimeOnly));

        // TODO convert the remaining resources to ResourceDefinition
        // Runtime addresses
        if (registerRuntimeOnly) {
            final ManagementResourceRegistration coreAddress = serverRegistration.registerSubModel(CORE_ADDRESS_PATH, MessagingSubsystemProviders.CORE_ADDRESS);
            coreAddress.setRuntimeOnly(true);
            AddressControlHandler.INSTANCE.register(coreAddress);
        }

        // Address settings
        final ManagementResourceRegistration addressSetting = serverRegistration.registerSubModel(ADDRESS_SETTING, MessagingSubsystemProviders.ADDRESS_SETTING);
        addressSetting.registerOperationHandler(ADD, AddressSettingAdd.INSTANCE, MessagingSubsystemProviders.ADDRESS_SETTING_ADD);
        addressSetting.registerOperationHandler(REMOVE, AddressSettingRemove.INSTANCE, MessagingSubsystemProviders.ADDRESS_SETTING_REMOVE);
        AddressSettingsWriteHandler.INSTANCE.registerAttributes(addressSetting, registerRuntimeOnly);

        // Broadcast groups
        final ManagementResourceRegistration broadcastGroups = serverRegistration.registerSubModel(BROADCAST_GROUP_PATH, MessagingSubsystemProviders.BROADCAST_GROUP_RESOURCE);
        broadcastGroups.registerOperationHandler(ADD, BroadcastGroupAdd.INSTANCE, BroadcastGroupAdd.INSTANCE);
        broadcastGroups.registerOperationHandler(REMOVE, BroadcastGroupRemove.INSTANCE, BroadcastGroupRemove.INSTANCE);
        BroadcastGroupWriteAttributeHandler.INSTANCE.registerAttributes(broadcastGroups, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            BroadcastGroupControlHandler.INSTANCE.register(broadcastGroups);
        }
        // getConnectorPairs, -- no, this is just the same as attribute connector-refs

        // Discovery groups
        final ManagementResourceRegistration discoveryGroups = serverRegistration.registerSubModel(DISCOVERY_GROUP_PATH, MessagingSubsystemProviders.DISCOVERY_GROUP_RESOURCE);
        discoveryGroups.registerOperationHandler(ADD, DiscoveryGroupAdd.INSTANCE, DiscoveryGroupAdd.INSTANCE);
        discoveryGroups.registerOperationHandler(REMOVE, DiscoveryGroupRemove.INSTANCE, DiscoveryGroupRemove.INSTANCE);
        DiscoveryGroupWriteAttributeHandler.INSTANCE.registerAttributes(discoveryGroups, registerRuntimeOnly);

        // Diverts
        final ManagementResourceRegistration diverts = serverRegistration.registerSubModel(DIVERT_PATH, MessagingSubsystemProviders.DIVERT_RESOURCE);
        diverts.registerOperationHandler(ADD, DivertAdd.INSTANCE, DivertAdd.INSTANCE);
        diverts.registerOperationHandler(REMOVE, DivertRemove.INSTANCE, DivertRemove.INSTANCE);
        DivertConfigurationWriteHandler.INSTANCE.registerAttributes(diverts, registerRuntimeOnly);

        // Core queues
        final ManagementResourceRegistration queue = serverRegistration.registerSubModel(PathElement.pathElement(QUEUE), MessagingSubsystemProviders.QUEUE_RESOURCE);
        queue.registerOperationHandler(ADD, QueueAdd.INSTANCE, QueueAdd.INSTANCE, false);
        queue.registerOperationHandler(REMOVE, QueueRemove.INSTANCE, QueueRemove.INSTANCE, false);
        QueueConfigurationWriteHandler.INSTANCE.registerAttributes(queue, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            QueueReadAttributeHandler.INSTANCE.registerAttributes(queue);
            QueueControlHandler.INSTANCE.registerOperations(queue);
        }
        // getExpiryAddress, setExpiryAddress, getDeadLetterAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting

        final ManagementResourceRegistration acceptor = serverRegistration.registerSubModel(GENERIC_ACCEPTOR, MessagingSubsystemProviders.ACCEPTOR);
        acceptor.registerOperationHandler(ADD, TransportConfigOperationHandlers.GENERIC_ADD, MessagingSubsystemProviders.ACCEPTOR_ADD);
        acceptor.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.ACCEPTOR_REMOVE);
        TransportConfigOperationHandlers.GENERIC_ATTR.registerAttributes(acceptor, registerRuntimeOnly);
        createParamRegistration(acceptor);
        if (registerRuntimeOnly) {
            AcceptorControlHandler.INSTANCE.register(acceptor);
        }


        // remote acceptor
        final ManagementResourceRegistration remoteAcceptor = serverRegistration.registerSubModel(REMOTE_ACCEPTOR, MessagingSubsystemProviders.REMOTE_ACCEPTOR);
        remoteAcceptor.registerOperationHandler(ADD, TransportConfigOperationHandlers.REMOTE_ADD, MessagingSubsystemProviders.REMOTE_ACCEPTOR_ADD);
        remoteAcceptor.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.ACCEPTOR_REMOVE);
        TransportConfigOperationHandlers.REMOTE_ATTR.registerAttributes(remoteAcceptor, registerRuntimeOnly);
        createParamRegistration(remoteAcceptor);
        if (registerRuntimeOnly) {
            AcceptorControlHandler.INSTANCE.register(remoteAcceptor);
        }

        // in-vm acceptor
        final ManagementResourceRegistration inVMAcceptor = serverRegistration.registerSubModel(IN_VM_ACCEPTOR, MessagingSubsystemProviders.IN_VM_ACCEPTOR);
        inVMAcceptor.registerOperationHandler(ADD, TransportConfigOperationHandlers.IN_VM_ADD, MessagingSubsystemProviders.IN_VM_ACCEPTOR_ADD);
        inVMAcceptor.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.ACCEPTOR_REMOVE);
        TransportConfigOperationHandlers.IN_VM_ATTR.registerAttributes(inVMAcceptor, registerRuntimeOnly);
        createParamRegistration(inVMAcceptor);
        if (registerRuntimeOnly) {
            AcceptorControlHandler.INSTANCE.register(inVMAcceptor);
        }

        // connector
        final ManagementResourceRegistration connector = serverRegistration.registerSubModel(GENERIC_CONNECTOR, MessagingSubsystemProviders.CONNECTOR);
        connector.registerOperationHandler(ADD, TransportConfigOperationHandlers.GENERIC_ADD, MessagingSubsystemProviders.CONNECTOR_ADD);
        connector.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.CONNECTOR_REMOVE);
        TransportConfigOperationHandlers.GENERIC_ATTR.registerAttributes(connector, registerRuntimeOnly);
        createParamRegistration(connector);

        // remote connector
        final ManagementResourceRegistration remoteConnector = serverRegistration.registerSubModel(REMOTE_CONNECTOR, MessagingSubsystemProviders.REMOTE_CONNECTOR);
        remoteConnector.registerOperationHandler(ADD, TransportConfigOperationHandlers.REMOTE_ADD, MessagingSubsystemProviders.REMOTE_CONNECTOR_ADD);
        remoteConnector.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.CONNECTOR_REMOVE);
        TransportConfigOperationHandlers.REMOTE_ATTR.registerAttributes(remoteConnector, registerRuntimeOnly);
        createParamRegistration(remoteConnector);

        // in-vm connector
        final ManagementResourceRegistration inVMConnector = serverRegistration.registerSubModel(IN_VM_CONNECTOR, MessagingSubsystemProviders.IN_VM_CONNECTOR);
        inVMConnector.registerOperationHandler(ADD, TransportConfigOperationHandlers.IN_VM_ADD, MessagingSubsystemProviders.IN_VM_CONNECTOR_ADD);
        inVMConnector.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.CONNECTOR_REMOVE);
        TransportConfigOperationHandlers.IN_VM_ATTR.registerAttributes(inVMConnector, registerRuntimeOnly);
        createParamRegistration(inVMConnector);

        // Bridges
        final ManagementResourceRegistration bridge = serverRegistration.registerSubModel(PathElement.pathElement(CommonAttributes.BRIDGE), MessagingSubsystemProviders.BRIDGE_RESOURCE);
        bridge.registerOperationHandler(ADD, BridgeAdd.INSTANCE, BridgeAdd.INSTANCE, false);
        bridge.registerOperationHandler(REMOVE, BridgeRemove.INSTANCE, BridgeRemove.INSTANCE, false);
        BridgeWriteAttributeHandler.INSTANCE.registerAttributes(bridge, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            BridgeControlHandler.INSTANCE.register(bridge);
        }

        // Cluster connections
        final ManagementResourceRegistration cluster = serverRegistration.registerSubModel(PathElement.pathElement(CommonAttributes.CLUSTER_CONNECTION),
                MessagingSubsystemProviders.CLUSTER_CONNECTION_RESOURCE);
        cluster.registerOperationHandler(ADD, ClusterConnectionAdd.INSTANCE, ClusterConnectionAdd.INSTANCE, false);
        cluster.registerOperationHandler(REMOVE, ClusterConnectionRemove.INSTANCE, ClusterConnectionRemove.INSTANCE, false);
        ClusterConnectionWriteAttributeHandler.INSTANCE.registerAttributes(cluster, registerRuntimeOnly);
        if (registerRuntimeOnly) {
            ClusterConnectionControlHandler.INSTANCE.register(cluster);
        }

        // Grouping Handler
        final ManagementResourceRegistration groupingHandler = serverRegistration.registerSubModel(GROUPING_HANDLER_PATH, MessagingSubsystemProviders.GROUPING_HANDLER_RESOURCE);
        groupingHandler.registerOperationHandler(ADD, GroupingHandlerAdd.INSTANCE, GroupingHandlerAdd.INSTANCE);
        groupingHandler.registerOperationHandler(REMOVE, GroupingHandlerRemove.INSTANCE, GroupingHandlerRemove.INSTANCE);
        GroupingHandlerWriteAttributeHandler.INSTANCE.registerAttributes(groupingHandler, registerRuntimeOnly);

        // Connector services
        final ManagementResourceRegistration connectorService = serverRegistration.registerSubModel(PathElement.pathElement(CommonAttributes.CONNECTOR_SERVICE),
                MessagingSubsystemProviders.CONNECTOR_SERVICE_RESOURCE);
        connectorService.registerOperationHandler(ADD, ConnectorServiceAdd.INSTANCE, ConnectorServiceAdd.INSTANCE, false);
        connectorService.registerOperationHandler(REMOVE, ConnectorServiceRemove.INSTANCE, ConnectorServiceRemove.INSTANCE, false);
        ConnectorServiceWriteAttributeHandler.INSTANCE.registerAttributes(connectorService, registerRuntimeOnly);

        final ManagementResourceRegistration connectorServiceParam = connectorService.registerSubModel(PathElement.pathElement(CommonAttributes.PARAM),
                MessagingSubsystemProviders.CONNECTOR_SERVICE_PARAM_RESOURCE);
        connectorServiceParam.registerOperationHandler(ADD, ConnectorServiceParamAdd.INSTANCE, ConnectorServiceParamAdd.INSTANCE, false);
        connectorServiceParam.registerOperationHandler(REMOVE, ConnectorServiceParamRemove.INSTANCE, ConnectorServiceParamRemove.INSTANCE, false);
        connectorServiceParam.registerReadWriteAttribute(CommonAttributes.VALUE.getName(), null, ConnectorServiceParamWriteAttributeHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

        // Messaging paths
        for (final String path : CommonAttributes.PATHS) {
            ManagementResourceRegistration bindings = serverRegistration.registerSubModel(PathElement.pathElement(PATH, path),
                    new MessagingSubsystemProviders.PathProvider(path));
            MessagingPathHandlers.register(bindings);
        }

        // Connection factories
        final ManagementResourceRegistration cfs = serverRegistration.registerSubModel(CFS_PATH, MessagingSubsystemProviders.CF);
        cfs.registerOperationHandler(ADD, ConnectionFactoryAdd.INSTANCE, MessagingSubsystemProviders.CF_ADD, false);
        cfs.registerOperationHandler(REMOVE, ConnectionFactoryRemove.INSTANCE, MessagingSubsystemProviders.CF_REMOVE, false);
        ConnectionFactoryWriteAttributeHandler.INSTANCE.registerAttributes(cfs);
        if (registerRuntimeOnly) {
            ConnectionFactoryReadAttributeHandler.INSTANCE.registerAttributes(cfs);
        }
        ConnectionFactoryAddJndiHandler.INSTANCE.registerOperation(cfs);
        // getJNDIBindings (no -- same as "entries")

        // Resource Adapter Pooled connection factories
        final ManagementResourceRegistration resourceAdapters = serverRegistration.registerSubModel(RA_PATH, MessagingSubsystemProviders.RA);
        resourceAdapters.registerOperationHandler(ADD, PooledConnectionFactoryAdd.INSTANCE, MessagingSubsystemProviders.RA_ADD, false);
        resourceAdapters.registerOperationHandler(REMOVE, PooledConnectionFactoryRemove.INSTANCE, MessagingSubsystemProviders.RA_REMOVE);
        PooledConnectionFactoryWriteAttributeHandler.INSTANCE.registerAttributes(resourceAdapters, registerRuntimeOnly);
        // TODO how do ConnectionFactoryControl things relate?

        // JMS Queues
        final ManagementResourceRegistration queues = serverRegistration.registerSubModel(JMS_QUEUE_PATH, MessagingSubsystemProviders.JMS_QUEUE_RESOURCE);
        queues.registerOperationHandler(ADD, JMSQueueAdd.INSTANCE, JMSQueueAdd.INSTANCE, false);
        queues.registerOperationHandler(REMOVE, JMSQueueRemove.INSTANCE, JMSQueueRemove.INSTANCE, false);
        JmsQueueConfigurationWriteHandler.INSTANCE.registerAttributes(queues, registerRuntimeOnly);
        JMSQueueAddJndiHandler.INSTANCE.registerOperation(queues);
        if (registerRuntimeOnly) {
            JmsQueueReadAttributeHandler.INSTANCE.registerAttributes(queues);
            JMSQueueControlHandler.INSTANCE.registerOperations(queues);
        }
        // setExpiryAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting
        // getJNDIBindings (no -- same as "entries")

        // JMS Topics
        final ManagementResourceRegistration topics = serverRegistration.registerSubModel(TOPIC_PATH, MessagingSubsystemProviders.JMS_TOPIC_RESOURCE);
        topics.registerOperationHandler(ADD, JMSTopicAdd.INSTANCE, JMSTopicAdd.INSTANCE, false);
        topics.registerOperationHandler(REMOVE, JMSTopicRemove.INSTANCE, JMSTopicRemove.INSTANCE, false);
        JMSTopicConfigurationWriteHandler.INSTANCE.registerAttributes(topics);
        JMSTopicAddJndiHandler.INSTANCE.registerOperation(topics);
        if (registerRuntimeOnly) {
            JMSTopicReadAttributeHandler.INSTANCE.registerAttributes(topics);
            JMSTopicControlHandler.INSTANCE.registerOperations(topics);
        }
        // getJNDIBindings (no -- same as "entries")

        final ManagementResourceRegistration securitySettings = serverRegistration.registerSubModel(SECURITY_SETTING, MessagingSubsystemProviders.SECURITY_SETTING);
        securitySettings.registerOperationHandler(ADD, SecuritySettingAdd.INSTANCE, SecuritySettingAdd.INSTANCE);
        securitySettings.registerOperationHandler(REMOVE, SecuritySettingRemove.INSTANCE, SecuritySettingRemove.INSTANCE);

        final ManagementResourceRegistration securityRole = securitySettings.registerSubModel(SECURITY_ROLE, MessagingSubsystemProviders.SECURITY_ROLE);
        securityRole.registerOperationHandler(ADD, SecurityRoleAdd.INSTANCE, SecurityRoleAdd.INSTANCE);
        securityRole.registerOperationHandler(REMOVE, SecurityRoleRemove.INSTANCE, SecurityRoleRemove.INSTANCE);
        SecurityRoleAttributeHandler.INSTANCE.registerAttributes(securityRole, registerRuntimeOnly);

        if (context.isRuntimeOnlyRegistrationValid()) {

            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME), getResourceDescriptionResolver("deployed"));
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            final ManagementResourceRegistration serverModel = deploymentsRegistration.registerSubModel(DeploymentHornetQServerResourceDefinition.INSTANCE);

            // JMS Queues
            final ManagementResourceRegistration deploymentQueue = serverModel.registerSubModel(JMS_QUEUE_PATH, MessagingSubsystemProviders.JMS_QUEUE_RESOURCE);
            JmsQueueReadAttributeHandler.INSTANCE.registerAttributes(deploymentQueue);
            JMSQueueControlHandler.INSTANCE.registerOperations(deploymentQueue);

            // topics
            final ManagementResourceRegistration deploymentTopics = serverModel.registerSubModel(TOPIC_PATH, MessagingSubsystemProviders.JMS_TOPIC_RESOURCE);
            JMSTopicReadAttributeHandler.INSTANCE.registerAttributes(deploymentTopics);
            JMSTopicControlHandler.INSTANCE.registerOperations(deploymentTopics);

        }
    }

    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace : Namespace.values()) {
            if (namespace == Namespace.UNKNOWN) {
                continue;
            }
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUriString(), MessagingSubsystemParser.getInstance());
        }
    }

    static void createParamRegistration(final ManagementResourceRegistration parent) {
        final ManagementResourceRegistration registration = parent.registerSubModel(PARAM, MessagingSubsystemProviders.PARAM);
        registration.registerOperationHandler(ADD, TransportConfigOperationHandlers.PARAM_ADD, MessagingSubsystemProviders.PARAM_ADD);
        registration.registerOperationHandler(REMOVE, TransportConfigOperationHandlers.REMOVE, MessagingSubsystemProviders.PARAM_REMOVE);
        registration.registerReadWriteAttribute("value", null, TransportConfigOperationHandlers.PARAM_ATTR, EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES));
    }

}
