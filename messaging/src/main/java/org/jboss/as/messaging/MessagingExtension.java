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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_0;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_1;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_2;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_3;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.messaging.jms.ConnectionFactoryAdd;
import org.jboss.as.messaging.jms.ConnectionFactoryAddJndiHandler;
import org.jboss.as.messaging.jms.ConnectionFactoryReadAttributeHandler;
import org.jboss.as.messaging.jms.ConnectionFactoryRemove;
import org.jboss.as.messaging.jms.ConnectionFactoryWriteAttributeHandler;
import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSQueueAddJndiHandler;
import org.jboss.as.messaging.jms.JMSQueueConfigurationRuntimeHandler;
import org.jboss.as.messaging.jms.JMSQueueConfigurationWriteHandler;
import org.jboss.as.messaging.jms.JMSQueueControlHandler;
import org.jboss.as.messaging.jms.JMSQueueReadAttributeHandler;
import org.jboss.as.messaging.jms.JMSQueueRemove;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.as.messaging.jms.JMSTopicAddJndiHandler;
import org.jboss.as.messaging.jms.JMSTopicConfigurationRuntimeHandler;
import org.jboss.as.messaging.jms.JMSTopicConfigurationWriteHandler;
import org.jboss.as.messaging.jms.JMSTopicControlHandler;
import org.jboss.as.messaging.jms.JMSTopicReadAttributeHandler;
import org.jboss.as.messaging.jms.JMSTopicRemove;
import org.jboss.as.messaging.jms.PooledConnectionFactoryAdd;
import org.jboss.as.messaging.jms.PooledConnectionFactoryRemove;
import org.jboss.as.messaging.jms.PooledConnectionFactoryWriteAttributeHandler;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;

/**
 * Domain extension that integrates HornetQ.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    static final String RESOURCE_NAME = MessagingDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private static final PathElement CORE_ADDRESS_PATH = PathElement.pathElement(CommonAttributes.CORE_ADDRESS);
    private static final PathElement CFS_PATH = PathElement.pathElement(CommonAttributes.CONNECTION_FACTORY);
    private static final PathElement JMS_QUEUE_PATH = PathElement.pathElement(CommonAttributes.JMS_QUEUE);
    private static final PathElement TOPIC_PATH = PathElement.pathElement(CommonAttributes.JMS_TOPIC);
    private static final PathElement RA_PATH = PathElement.pathElement(CommonAttributes.POOLED_CONNECTION_FACTORY);

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return getResourceDescriptionResolver(keyPrefix, true);
    }

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix, final boolean useUnprefixedChildTypes) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }


    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerXMLElementWriter(Messaging13SubsystemParser.getInstance());

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Root resource
        final ManagementResourceRegistration rootRegistration = subsystem.registerSubsystemModel(MessagingSubsystemRootResourceDefinition.INSTANCE);
        rootRegistration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // HQ servers
        final ManagementResourceRegistration serverRegistration = rootRegistration.registerSubModel(new HornetQServerResourceDefinition(registerRuntimeOnly));

        // Runtime addresses
        if (registerRuntimeOnly) {
            final ManagementResourceRegistration coreAddress = serverRegistration.registerSubModel(new CoreAddressDefinition());
            coreAddress.setRuntimeOnly(true);
        }

        // Address settings
        serverRegistration.registerSubModel(new AddressSettingDefinition(registerRuntimeOnly));

        // Broadcast groups
        serverRegistration.registerSubModel(new BroadcastGroupDefinition(registerRuntimeOnly));
        // getConnectorPairs, -- no, this is just the same as attribute connector-refs

        // Discovery groups
        serverRegistration.registerSubModel(new DiscoveryGroupDefinition(registerRuntimeOnly));

        // Diverts
        serverRegistration.registerSubModel(new DivertDefinition(registerRuntimeOnly));

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

        // Acceptors
        serverRegistration.registerSubModel(GenericTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(RemoteTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(InVMTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));

        // Connectors
        serverRegistration.registerSubModel(GenericTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(RemoteTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(InVMTransportDefinition.createConnectorDefinition(registerRuntimeOnly));

        // Bridges
        serverRegistration.registerSubModel(new BridgeDefinition(registerRuntimeOnly));

        // Cluster connections
        serverRegistration.registerSubModel(new ClusterConnectionDefinition(registerRuntimeOnly));

        // Grouping Handler
        serverRegistration.registerSubModel(new GroupingHandlerDefinition(registerRuntimeOnly));

        // Connector services
        serverRegistration.registerSubModel(new ConnectorServiceDefinition(registerRuntimeOnly));

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
        JMSQueueConfigurationWriteHandler.INSTANCE.registerAttributes(queues, registerRuntimeOnly);
        JMSQueueAddJndiHandler.INSTANCE.registerOperation(queues);
        if (registerRuntimeOnly) {
            JMSQueueReadAttributeHandler.INSTANCE.registerAttributes(queues);
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

        serverRegistration.registerSubModel(new SecuritySettingDefinition(registerRuntimeOnly));

        if (context.isRuntimeOnlyRegistrationValid()) {

            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME), getResourceDescriptionResolver("deployed"));
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            final ManagementResourceRegistration serverModel = deploymentsRegistration.registerSubModel(new HornetQServerResourceDefinition(true));

            // JMS Queues
            final ManagementResourceRegistration deploymentQueue = serverModel.registerSubModel(JMS_QUEUE_PATH, MessagingSubsystemProviders.JMS_QUEUE_RESOURCE);
            JMSQueueReadAttributeHandler.INSTANCE.registerAttributes(deploymentQueue);
            JMSQueueControlHandler.INSTANCE.registerOperations(deploymentQueue);
            JMSQueueConfigurationRuntimeHandler.INSTANCE.registerAttributes(deploymentQueue);

            // topics
            final ManagementResourceRegistration deploymentTopics = serverModel.registerSubModel(TOPIC_PATH, MessagingSubsystemProviders.JMS_TOPIC_RESOURCE);
            JMSTopicReadAttributeHandler.INSTANCE.registerAttributes(deploymentTopics);
            JMSTopicControlHandler.INSTANCE.registerOperations(deploymentTopics);
            JMSTopicConfigurationRuntimeHandler.INSTANCE.registerAttributes(deploymentTopics);

        }

        // JMS Bridges
        rootRegistration.registerSubModel(new JMSBridgeDefinition());
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_0.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_1.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_2.getUriString(), Messaging12SubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_3.getUriString(), Messaging13SubsystemParser.getInstance());
    }
}
