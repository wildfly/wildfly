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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.transform.OperationResultTransformer.ORIGINAL_RESULT;
import static org.jboss.as.messaging.CommonAttributes.BACKUP_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CHECK_FOR_LIVE_SERVER;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CORE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REPLICATION_CLUSTERNAME;
import static org.jboss.as.messaging.CommonAttributes.RUNTIME_QUEUE;
import static org.jboss.as.messaging.GroupingHandlerDefinition.TYPE;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_0;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_1;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_2;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_3;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_AUTO_RECOVERY;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Domain extension that integrates HornetQ.
 *
 * <dl>
 *   <dt>AS 7.2.0</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.3
 *       <li>Management model: 1.2.0
 *     </ul>
 *   </dd>
 *   <dt>AS 7.1.2, 7.1.3<dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.2
 *       <li>Management model: 1.1.0
 *     </ul>
 *   </dd>
 * </dl>
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    static final PathElement SUBSYSTEM_PATH  = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final String RESOURCE_NAME = MessagingExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    public static final ModelVersion VERSION_1_2_0 = ModelVersion.create(1, 2, 0);
    public static final ModelVersion VERSION_1_1_0 = ModelVersion.create(1, 1, 0);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);
    }

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME,
                MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION,
                MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerXMLElementWriter(MessagingXMLWriter.INSTANCE);

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Root resource
        final ManagementResourceRegistration rootRegistration = subsystem.registerSubsystemModel(MessagingSubsystemRootResourceDefinition.INSTANCE);
        rootRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

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
        serverRegistration.registerSubModel(QueueDefinition.newQueueDefinition(registerRuntimeOnly));
        // getExpiryAddress, setExpiryAddress, getDeadLetterAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting

        // Runtime core queues
        serverRegistration.registerSubModel(QueueDefinition.newRuntimeQueueDefinition(registerRuntimeOnly));

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
        //todo, shouldn't we leverage Path service from AS? see: package org.jboss.as.controller.services.path
        for (final String path : MessagingPathHandlers.PATHS.keySet()) {
            ManagementResourceRegistration bindings = serverRegistration.registerSubModel(PathElement.pathElement(PATH, path),
                    new MessagingSubsystemProviders.PathProvider(path));
            MessagingPathHandlers.register(bindings, path);
            // Create the path resolver operation
            if (context.getProcessType().isServer()) {
                final ResolvePathHandler resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                        .setPathAttribute(MessagingPathHandlers.PATHS.get(path))
                        .setRelativeToAttribute(MessagingPathHandlers.RELATIVE_TO)
                        .build();
                bindings.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
            }
        }

        // Connection factories
        serverRegistration.registerSubModel(new ConnectionFactoryDefinition(registerRuntimeOnly));
        // getJNDIBindings (no -- same as "entries")

        // Resource Adapter Pooled connection factories
        serverRegistration.registerSubModel(new PooledConnectionFactoryDefinition(registerRuntimeOnly));
        // TODO how do ConnectionFactoryControl things relate?

        // JMS Queues
        serverRegistration.registerSubModel(new JMSQueueDefinition(registerRuntimeOnly));
        // setExpiryAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting
        // getJNDIBindings (no -- same as "entries")

        // JMS Topics
        serverRegistration.registerSubModel(new JMSTopicDefinition(registerRuntimeOnly));
        // getJNDIBindings (no -- same as "entries")

        serverRegistration.registerSubModel(new SecuritySettingDefinition(registerRuntimeOnly));

        if (registerRuntimeOnly) {

            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(SUBSYSTEM_PATH, getResourceDescriptionResolver("deployed"));
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            final ManagementResourceRegistration serverModel = deploymentsRegistration.registerSubModel(new HornetQServerResourceDefinition(true));

            serverModel.registerSubModel(JMSQueueDefinition.newDeployedJMSQueueDefinition());
            serverModel.registerSubModel(JMSTopicDefinition.newDeployedJMSTopicDefinition());
        }

        // JMS Bridges
        rootRegistration.registerSubModel(new JMSBridgeDefinition());

        registerTransformers_1_1_0(subsystem);
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_0.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_1.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_2.getUriString(), Messaging12SubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_3.getUriString(), Messaging13SubsystemParser.getInstance());
    }

    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {

        final AttributeDefinition[] transformerdPooledCFAttributes = { Pooled.INITIAL_CONNECT_ATTEMPTS,
                Pooled.INITIAL_MESSAGE_PACKET_SIZE,
                COMPRESS_LARGE_MESSAGES,
                USE_AUTO_RECOVERY,
                CALL_FAILOVER_TIMEOUT };

        final TransformersSubRegistration transformers = subsystem.registerModelTransformers(VERSION_1_1_0, new AbstractSubsystemTransformer(SUBSYSTEM_NAME) {

            @Override
            public ModelNode transformModel(final TransformationContext context, final ModelNode model) {
                ModelNode oldModel = model.clone();
                if (oldModel.hasDefined(HORNETQ_SERVER)) {
                    for (Property server : oldModel.get(HORNETQ_SERVER).asPropertyList()) {
                        if (!oldModel.get(HORNETQ_SERVER, server.getName()).hasDefined(CLUSTERED.getName())) {
                            oldModel.get(HORNETQ_SERVER, server.getName()).get(CLUSTERED.getName()).set(false);
                        }
                        oldModel.get(HORNETQ_SERVER, server.getName()).remove(CHECK_FOR_LIVE_SERVER.getName());
                        oldModel.get(HORNETQ_SERVER, server.getName()).remove(BACKUP_GROUP_NAME.getName());
                        oldModel.get(HORNETQ_SERVER, server.getName()).remove(REPLICATION_CLUSTERNAME.getName());
                        if (server.getValue().hasDefined(CLUSTER_CONNECTION)) {
                            for (Property clusterConnection : server.getValue().get(CLUSTER_CONNECTION).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), CLUSTER_CONNECTION, clusterConnection.getName()).remove(CALL_FAILOVER_TIMEOUT.getName());
                            }
                        }
                        if (server.getValue().hasDefined(BROADCAST_GROUP)) {
                            for (Property broadcastGroup : server.getValue().get(BROADCAST_GROUP).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), BROADCAST_GROUP, broadcastGroup.getName()).remove(JGROUPS_STACK.getName());
                                oldModel.get(HORNETQ_SERVER, server.getName(), BROADCAST_GROUP, broadcastGroup.getName()).remove(JGROUPS_CHANNEL.getName());
                            }
                        }
                        if (server.getValue().hasDefined(DISCOVERY_GROUP)) {
                            for (Property discoveryGroup : server.getValue().get(DISCOVERY_GROUP).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), DISCOVERY_GROUP, discoveryGroup.getName()).remove(JGROUPS_STACK.getName());
                                oldModel.get(HORNETQ_SERVER, server.getName(), DISCOVERY_GROUP, discoveryGroup.getName()).remove(JGROUPS_CHANNEL.getName());
                            }
                        }
                        if (server.getValue().hasDefined(POOLED_CONNECTION_FACTORY)) {
                            for (Property pooledConnectionFactory : server.getValue().get(POOLED_CONNECTION_FACTORY).asPropertyList()) {
                                for (AttributeDefinition attribute : transformerdPooledCFAttributes) {
                                    oldModel.get(HORNETQ_SERVER, server.getName(), POOLED_CONNECTION_FACTORY, pooledConnectionFactory.getName()).remove(attribute.getName());
                                }
                            }
                        }
                        if (server.getValue().hasDefined(CONNECTION_FACTORY)) {
                            for (Property connectionFactory : server.getValue().get(CONNECTION_FACTORY).asPropertyList()) {
                                oldModel.get(HORNETQ_SERVER, server.getName(), CONNECTION_FACTORY, connectionFactory.getName()).remove(CALL_FAILOVER_TIMEOUT.getName());
                                if (!connectionFactory.getValue().hasDefined(HA.getName())) {
                                    oldModel.get(HORNETQ_SERVER, server.getName(), CONNECTION_FACTORY, connectionFactory.getName()).get(HA.getName()).set(HA.getDefaultValue());
                                }
                                if (connectionFactory.getValue().hasDefined(FACTORY_TYPE.getName()) && (connectionFactory.getValue().get(FACTORY_TYPE.getName()).equals(FACTORY_TYPE.getDefaultValue()))) {
                                    oldModel.get(HORNETQ_SERVER, server.getName(), CONNECTION_FACTORY, connectionFactory.getName()).get(FACTORY_TYPE.getName()).set(new ModelNode());
                                }
                            }
                        }
                        //TODO - a nicer way to automagically remove these runtime resources?
                        if (server.getValue().hasDefined(CORE_ADDRESS)) {
                            oldModel.get(HORNETQ_SERVER, server.getName()).remove(CORE_ADDRESS);
                        }
                        if (server.getValue().hasDefined(RUNTIME_QUEUE)) {
                            oldModel.get(HORNETQ_SERVER, server.getName()).remove(RUNTIME_QUEUE);
                        }
                    }
                }
                return oldModel;
            }
        });

        TransformersSubRegistration server = transformers.registerSubResource(PathElement.pathElement(HORNETQ_SERVER));
        server.registerOperationTransformer(ADD,new OperationTransformers.MultipleOperationalTransformer(
                new OperationTransformers.InsertDefaultValuesOperationTransformer(ID_CACHE_SIZE, CLUSTERED),
                new OperationTransformers.RemoveAttributesOperationTransformer(CHECK_FOR_LIVE_SERVER, BACKUP_GROUP_NAME, REPLICATION_CLUSTERNAME)));

        RejectExpressionValuesTransformer rejectTransportParamExpressionTransformer = new RejectExpressionValuesTransformer(VALUE);
        final String[] transports = { CommonAttributes.ACCEPTOR, CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.IN_VM_ACCEPTOR,
                CommonAttributes.CONNECTOR, CommonAttributes.REMOTE_CONNECTOR, CommonAttributes.IN_VM_CONNECTOR };
        for (String transport : transports) {
            TransformersSubRegistration remoteConnector = server.registerSubResource(PathElement.pathElement(transport));
            TransformersSubRegistration transportParam = remoteConnector.registerSubResource(PathElement.pathElement(PARAM));
            transportParam.registerOperationTransformer(ADD, rejectTransportParamExpressionTransformer);
            transportParam.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectTransportParamExpressionTransformer.getWriteAttributeTransformer());
        }

        RejectExpressionValuesTransformer rejectExpressionTransformer = new RejectExpressionValuesTransformer(PATH);
        for (final String path : MessagingPathHandlers.PATHS.keySet()) {
            TransformersSubRegistration pathRegistration = server.registerSubResource(PathElement.pathElement(PATH, path), rejectExpressionTransformer, rejectExpressionTransformer);
            pathRegistration.registerOperationTransformer(ADD, rejectExpressionTransformer);
            pathRegistration.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectExpressionTransformer.getWriteAttributeTransformer());
        }

        TransformersSubRegistration clusterConnection = server.registerSubResource(ClusterConnectionDefinition.PATH);
        clusterConnection.registerOperationTransformer(ADD, new OperationTransformers.RemoveAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT));
        clusterConnection.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformers.FailUnignoredAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT));

        TransformersSubRegistration connectionFactory = server.registerSubResource(ConnectionFactoryDefinition.PATH);
        connectionFactory.registerOperationTransformer(ADD, new OperationTransformers.RemoveAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT));
        connectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformers.FailUnignoredAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT));

        TransformersSubRegistration pooledConnectionFactory = server.registerSubResource(PooledConnectionFactoryDefinition.PATH);
        pooledConnectionFactory.registerOperationTransformer(ADD, new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                    throws OperationFailedException {
                final ModelNode transformedOperation = operation.clone();
                for (AttributeDefinition attribute : transformerdPooledCFAttributes) {
                    transformedOperation.remove(attribute.getName());
                }
                if (!transformedOperation.hasDefined(RECONNECT_ATTEMPTS.getName())) {
                    transformedOperation.get(RECONNECT_ATTEMPTS.getName()).set(RECONNECT_ATTEMPTS.getDefaultValue());
                }

                return new TransformedOperation(transformedOperation, ORIGINAL_RESULT);
            }
        });
        pooledConnectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new OperationTransformers.FailUnignoredAttributesOperationTransformer(transformerdPooledCFAttributes));

        RejectExpressionValuesTransformer rejectTypeExpressionTransformer = new RejectExpressionValuesTransformer(TYPE);
        TransformersSubRegistration groupingHandler = server.registerSubResource(GroupingHandlerDefinition.PATH);
        groupingHandler.registerOperationTransformer(ADD, rejectTypeExpressionTransformer);
        groupingHandler.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectTypeExpressionTransformer.getWriteAttributeTransformer());
    }
}
