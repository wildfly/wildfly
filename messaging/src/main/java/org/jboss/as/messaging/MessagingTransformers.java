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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.messaging.BroadcastGroupDefinition.BROADCAST_PERIOD;
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
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REPLICATION_CLUSTERNAME;
import static org.jboss.as.messaging.CommonAttributes.RUNTIME_QUEUE;
import static org.jboss.as.messaging.DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT;
import static org.jboss.as.messaging.DiscoveryGroupDefinition.REFRESH_TIMEOUT;
import static org.jboss.as.messaging.MessagingExtension.SUBSYSTEM_NAME;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Pooled.USE_AUTO_RECOVERY;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.AbstractSubsystemTransformer;
import org.jboss.as.controller.transform.DiscardAttributesTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Resource transformations for the messaging subsystem.
 *
 * <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc.
 */
public class MessagingTransformers {

    static void registerTransformers(final SubsystemRegistration subsystem) {
        registerTransformers_1_1_0(subsystem);
    }

    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {

        // attributes added to the pooled cf resources *after* 1.1.0
        final AttributeDefinition[] addedPooledCFAttributes = { ConnectionFactoryAttributes.Pooled.INITIAL_CONNECT_ATTEMPTS,
                ConnectionFactoryAttributes.Pooled.INITIAL_MESSAGE_PACKET_SIZE,
                COMPRESS_LARGE_MESSAGES,
                USE_AUTO_RECOVERY,
                CALL_FAILOVER_TIMEOUT };

        final TransformersSubRegistration transformers = subsystem.registerModelTransformers(VERSION_1_1_0, new AbstractSubsystemTransformer(SUBSYSTEM_NAME) {

            @Override
            public ModelNode transformModel(final TransformationContext context, final ModelNode model) {
                ModelNode oldModel = model.clone();
                if (oldModel.hasDefined(HORNETQ_SERVER)) {
                    for (Property server : oldModel.get(HORNETQ_SERVER).asPropertyList()) {
                        ModelNode oldServer = oldModel.get(HORNETQ_SERVER, server.getName());
                        if (!oldServer.hasDefined(CLUSTERED.getName())) {
                            oldServer.get(CLUSTERED.getName()).set(false);
                        }
                        oldServer.remove(CHECK_FOR_LIVE_SERVER.getName());
                        oldServer.remove(BACKUP_GROUP_NAME.getName());
                        oldServer.remove(REPLICATION_CLUSTERNAME.getName());
                        if (server.getValue().hasDefined(CLUSTER_CONNECTION)) {
                            for (Property clusterConnection : server.getValue().get(CLUSTER_CONNECTION).asPropertyList()) {
                                oldServer.get(HORNETQ_SERVER, server.getName(), CLUSTER_CONNECTION, clusterConnection.getName()).remove(CALL_FAILOVER_TIMEOUT.getName());
                            }
                        }
                        if (server.getValue().hasDefined(BROADCAST_GROUP)) {
                            for (Property broadcastGroup : server.getValue().get(BROADCAST_GROUP).asPropertyList()) {
                                oldServer.get(BROADCAST_GROUP, broadcastGroup.getName()).remove(JGROUPS_STACK.getName());
                                oldServer.get(BROADCAST_GROUP, broadcastGroup.getName()).remove(JGROUPS_CHANNEL.getName());
                            }
                        }
                        if (server.getValue().hasDefined(DISCOVERY_GROUP)) {
                            for (Property discoveryGroup : server.getValue().get(DISCOVERY_GROUP).asPropertyList()) {
                                oldServer.get(DISCOVERY_GROUP, discoveryGroup.getName()).remove(JGROUPS_STACK.getName());
                                oldServer.get(DISCOVERY_GROUP, discoveryGroup.getName()).remove(JGROUPS_CHANNEL.getName());
                            }
                        }
                        if (server.getValue().hasDefined(POOLED_CONNECTION_FACTORY)) {
                            for (Property pooledConnectionFactory : server.getValue().get(POOLED_CONNECTION_FACTORY).asPropertyList()) {
                                for (AttributeDefinition attribute : addedPooledCFAttributes) {
                                    oldServer.get(POOLED_CONNECTION_FACTORY, pooledConnectionFactory.getName()).remove(attribute.getName());
                                }
                            }
                        }
                        if (server.getValue().hasDefined(CONNECTION_FACTORY)) {
                            for (Property connectionFactory : server.getValue().get(CONNECTION_FACTORY).asPropertyList()) {
                                oldServer.get(CONNECTION_FACTORY, connectionFactory.getName()).remove(CALL_FAILOVER_TIMEOUT.getName());
                                if (!connectionFactory.getValue().hasDefined(HA.getName())) {
                                    oldServer.get(CONNECTION_FACTORY, connectionFactory.getName()).get(HA.getName()).set(HA.getDefaultValue());
                                }
                                if (connectionFactory.getValue().hasDefined(FACTORY_TYPE.getName()) && (connectionFactory.getValue().get(FACTORY_TYPE.getName()).equals(FACTORY_TYPE.getDefaultValue()))) {
                                    oldServer.get(CONNECTION_FACTORY, connectionFactory.getName()).get(FACTORY_TYPE.getName()).set(new ModelNode());
                                }
                            }
                        }
                        //TODO - a nicer way to automagically remove these runtime resources?
                        if (server.getValue().hasDefined(CORE_ADDRESS)) {
                            oldServer.remove(CORE_ADDRESS);
                        }
                        if (server.getValue().hasDefined(RUNTIME_QUEUE)) {
                            oldServer.remove(RUNTIME_QUEUE);
                        }
                    }
                }
                return oldModel;
            }
        });

        RejectExpressionValuesTransformer rejectServerExpressionTransformer = new RejectExpressionValuesTransformer(HornetQServerResourceDefinition.REJECTED_EXPRESSION_ATTRIBUTES);
        MessagingDiscardAttributesTransformer discardNewServerAttributes = new MessagingDiscardAttributesTransformer(CHECK_FOR_LIVE_SERVER, BACKUP_GROUP_NAME, REPLICATION_CLUSTERNAME);
        TransformersSubRegistration server = transformers.registerSubResource(PathElement.pathElement(HORNETQ_SERVER));
        server.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                new OperationTransformers.InsertDefaultValuesOperationTransformer(ID_CACHE_SIZE, CLUSTERED),
                rejectServerExpressionTransformer,
                discardNewServerAttributes));
        server.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectServerExpressionTransformer.getWriteAttributeTransformer());
        server.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, discardNewServerAttributes.getUndefineAttributeTransformer());

        rejectExpressions(server, AddressSettingDefinition.PATH, AddressSettingDefinition.REJECTED_EXPRESSION_ATTRIBUTES);

        MessagingDiscardAttributesTransformer discardNewJGroupsAttributes = new MessagingDiscardAttributesTransformer(JGROUPS_CHANNEL, JGROUPS_STACK);

        RejectExpressionValuesTransformer rejectBroadcastGroupExpressions = new RejectExpressionValuesTransformer(BROADCAST_PERIOD);
        TransformersSubRegistration broadcastGroup = server.registerSubResource(BroadcastGroupDefinition.PATH);
        broadcastGroup.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectBroadcastGroupExpressions,
                discardNewJGroupsAttributes
        ));
        broadcastGroup.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectBroadcastGroupExpressions.getWriteAttributeTransformer(),
                discardNewJGroupsAttributes.getWriteAttributeTransformer()));
        broadcastGroup.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, discardNewJGroupsAttributes.getUndefineAttributeTransformer());

        RejectExpressionValuesTransformer rejectDiscoveryGroupExpressions = new RejectExpressionValuesTransformer(INITIAL_WAIT_TIMEOUT, REFRESH_TIMEOUT);
        TransformersSubRegistration discoveryGroup = server.registerSubResource(DiscoveryGroupDefinition.PATH);
        discoveryGroup.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectDiscoveryGroupExpressions,
                discardNewJGroupsAttributes
        ));
        discoveryGroup.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectDiscoveryGroupExpressions.getWriteAttributeTransformer(),
                discardNewJGroupsAttributes.getWriteAttributeTransformer()));
        discoveryGroup.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, discardNewJGroupsAttributes.getUndefineAttributeTransformer());

        rejectExpressions(server, DivertDefinition.PATH, DivertDefinition.REJECTED_EXPRESSION_ATTRIBUTES);

        rejectExpressions(server, BridgeDefinition.PATH, BridgeDefinition.REJECTED_EXPRESSION_ATTRIBUTES);

        rejectExpressions(server, QueueDefinition.PATH, QueueDefinition.REJECTED_EXPRESSION_ATTRIBUTES);

        for (String path :  new String[]{ CommonAttributes.ACCEPTOR, CommonAttributes.CONNECTOR }) {
            TransformersSubRegistration transport = rejectExpressions(server, PathElement.pathElement(path), CommonAttributes.FACTORY_CLASS);
            rejectExpressions(transport, TransportParamDefinition.PATH, VALUE);
        }

        for (String path :  new String[]{ CommonAttributes.IN_VM_ACCEPTOR, CommonAttributes.IN_VM_CONNECTOR }) {
            TransformersSubRegistration transport = rejectExpressions(server, PathElement.pathElement(path), InVMTransportDefinition.SERVER_ID);
            rejectExpressions(transport, TransportParamDefinition.PATH, VALUE);
        }

        for (String path :  new String[]{ CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.REMOTE_CONNECTOR }) {
            TransformersSubRegistration transport = server.registerSubResource(PathElement.pathElement(path));
            rejectExpressions(transport, TransportParamDefinition.PATH, VALUE);
        }

        for (final String path : MessagingPathHandlers.PATHS.keySet()) {
            rejectExpressions(server, PathElement.pathElement(PATH, path), PATH);
        }

        RejectExpressionValuesTransformer rejectClusterConnectionExpressions = new RejectExpressionValuesTransformer(ClusterConnectionDefinition.REJECTED_EXPRESSION_ATTRIBUTES);
        MessagingDiscardAttributesTransformer discardCallFailoverTimeoutAttribute = new MessagingDiscardAttributesTransformer(CALL_FAILOVER_TIMEOUT);
        TransformersSubRegistration clusterConnection = server.registerSubResource(ClusterConnectionDefinition.PATH, rejectClusterConnectionExpressions, rejectClusterConnectionExpressions);
        clusterConnection.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectClusterConnectionExpressions,
                discardCallFailoverTimeoutAttribute));
        clusterConnection.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectClusterConnectionExpressions.getWriteAttributeTransformer(),
                new OperationTransformers.FailUnignoredAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT)));
        clusterConnection.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, discardCallFailoverTimeoutAttribute.getUndefineAttributeTransformer());

        TransformersSubRegistration connectorService = rejectExpressions(server, ConnectorServiceDefinition.PATH, CommonAttributes.FACTORY_CLASS);
        rejectExpressions(connectorService, ConnectorServiceParamDefinition.PATH, VALUE);

        RejectExpressionValuesTransformer rejectConnectionFactoryExpressions = new RejectExpressionValuesTransformer(ConnectionFactoryDefinition.REJECTED_EXPRESSION_ATTRIBUTES);
        TransformersSubRegistration connectionFactory = server.registerSubResource(ConnectionFactoryDefinition.PATH, rejectConnectionFactoryExpressions, rejectConnectionFactoryExpressions);
        connectionFactory.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectConnectionFactoryExpressions,
                discardCallFailoverTimeoutAttribute));
        connectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectConnectionFactoryExpressions.getWriteAttributeTransformer(),
                new OperationTransformers.FailUnignoredAttributesOperationTransformer(CALL_FAILOVER_TIMEOUT)));
        connectionFactory.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, discardCallFailoverTimeoutAttribute.getUndefineAttributeTransformer());

        RejectExpressionValuesTransformer rejectPooledConnectionFactoryExpressions = new RejectExpressionValuesTransformer(PooledConnectionFactoryDefinition.REJECTED_EXPRESSION_ATTRIBUTES);
        DiscardAttributesTransformer discardNewPooledConnectionFactoryAttributes = new MessagingDiscardAttributesTransformer(addedPooledCFAttributes);
        ChainedOperationTransformer chainedPooledConnectionFactoryOps = new ChainedOperationTransformer(
                rejectConnectionFactoryExpressions,
                discardNewPooledConnectionFactoryAttributes,
                new OperationTransformers.InsertDefaultValuesOperationTransformer(ConnectionFactoryAttributes.Pooled.RECONNECT_ATTEMPTS));
        TransformersSubRegistration pooledConnectionFactory = server.registerSubResource(PooledConnectionFactoryDefinition.PATH, rejectPooledConnectionFactoryExpressions, chainedPooledConnectionFactoryOps);
        pooledConnectionFactory.registerOperationTransformer(ADD, chainedPooledConnectionFactoryOps);
        pooledConnectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectPooledConnectionFactoryExpressions.getWriteAttributeTransformer(),
                new OperationTransformers.FailUnignoredAttributesOperationTransformer(addedPooledCFAttributes)));
        pooledConnectionFactory.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                discardNewPooledConnectionFactoryAttributes.getUndefineAttributeTransformer()));

        rejectExpressions(server, GroupingHandlerDefinition.PATH, GroupingHandlerDefinition.REJECTED_EXPRESSION_ATTRIBUTES);

        rejectExpressions(server, JMSQueueDefinition.PATH, JMSQueueDefinition.REJECTED_EXPRESSION_ATTRIBUTES);
    }

    private static TransformersSubRegistration rejectExpressions(TransformersSubRegistration parent, PathElement path, AttributeDefinition... attributes) {
        final String[] names = new String[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            AttributeDefinition def = attributes[i];
            names[i] = def.getName();
        }
        return rejectExpressions(parent, path, names);
    }

    private static TransformersSubRegistration rejectExpressions(TransformersSubRegistration parent, PathElement path, String... attributes) {
        RejectExpressionValuesTransformer rejectExpressions = new RejectExpressionValuesTransformer(attributes);
        TransformersSubRegistration resource = parent.registerSubResource(path, rejectExpressions, rejectExpressions);
        resource.registerOperationTransformer(ADD, rejectExpressions);
        resource.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectExpressions.getWriteAttributeTransformer());
        return resource;
    }

    private static class MessagingDiscardAttributesTransformer extends DiscardAttributesTransformer {
        MessagingDiscardAttributesTransformer(AttributeDefinition...attributeDefinitions){
            super(attributeDefinitions);
        }
    }

}
