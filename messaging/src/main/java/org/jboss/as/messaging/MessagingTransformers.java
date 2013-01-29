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
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CORE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.RUNTIME_QUEUE;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Regular.FACTORY_TYPE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.DiscardUndefinedAttributesTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Resource transformations for the messaging subsystem.
 * <p/>
 * <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat, inc.
 */
public class MessagingTransformers {

    static void registerTransformers(final SubsystemRegistration subsystem) {
        registerTransformers_1_1_0(subsystem);
    }

    private static void registerTransformers_1_1_0(final SubsystemRegistration subsystem) {

        final TransformersSubRegistration transformers = subsystem.registerModelTransformers(VERSION_1_1_0, new ResourceTransformer() {

            private void removeAttributes(ModelNode model, AttributeDefinition... removedAttributes) {
                for (AttributeDefinition attr : removedAttributes) {
                    model.remove(attr.getName());
                }
            }

            @Override
            public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
                ModelNode oldModel = resource.getModel();
                if (oldModel.hasDefined(HORNETQ_SERVER)) {
                    for (Property server : oldModel.get(HORNETQ_SERVER).asPropertyList()) {
                        ModelNode oldServer = oldModel.get(HORNETQ_SERVER, server.getName());
                        if (!oldServer.hasDefined(CLUSTERED.getName())) {
                            oldServer.get(CLUSTERED.getName()).set(false);
                        }
                        removeAttributes(oldServer, HornetQServerResourceDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);

                        if (server.getValue().hasDefined(CLUSTER_CONNECTION)) {
                            for (Property clusterConnection : server.getValue().get(CLUSTER_CONNECTION).asPropertyList()) {
                                removeAttributes(oldServer.get(CLUSTER_CONNECTION, clusterConnection.getName()), ClusterConnectionDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
                            }
                        }
                        if (server.getValue().hasDefined(BROADCAST_GROUP)) {
                            for (Property broadcastGroup : server.getValue().get(BROADCAST_GROUP).asPropertyList()) {
                                removeAttributes(oldServer.get(BROADCAST_GROUP, broadcastGroup.getName()), BroadcastGroupDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
                            }
                        }
                        if (server.getValue().hasDefined(DISCOVERY_GROUP)) {
                            for (Property discoveryGroup : server.getValue().get(DISCOVERY_GROUP).asPropertyList()) {
                                removeAttributes(oldServer.get(DISCOVERY_GROUP, discoveryGroup.getName()), DiscoveryGroupDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
                            }
                        }
                        if (server.getValue().hasDefined(POOLED_CONNECTION_FACTORY)) {
                            for (Property pooledConnectionFactory : server.getValue().get(POOLED_CONNECTION_FACTORY).asPropertyList()) {
                                removeAttributes(oldServer.get(POOLED_CONNECTION_FACTORY, pooledConnectionFactory.getName()), PooledConnectionFactoryDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
                            }
                        }
                        if (server.getValue().hasDefined(CONNECTION_FACTORY)) {
                            for (Property connectionFactory : server.getValue().get(CONNECTION_FACTORY).asPropertyList()) {
                                removeAttributes(oldServer.get(CONNECTION_FACTORY, connectionFactory.getName()), ConnectionFactoryDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
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
                resource.writeModel(oldModel);
            }
        });

        RejectExpressionValuesTransformer rejectServerExpressionTransformer = new RejectExpressionValuesTransformer(HornetQServerResourceDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        DiscardUndefinedAttributesTransformer discardUndefinedServerAttributes = new DiscardUndefinedAttributesTransformer(HornetQServerResourceDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
        TransformersSubRegistration server = transformers.registerSubResource(PathElement.pathElement(HORNETQ_SERVER));
        server.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                new OperationTransformers.InsertDefaultValuesOperationTransformer(ID_CACHE_SIZE, CLUSTERED),
                rejectServerExpressionTransformer,
                discardUndefinedServerAttributes));
        server.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectServerExpressionTransformer.getWriteAttributeTransformer(),
                discardUndefinedServerAttributes.getWriteAttributeTransformer()));
        server.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                discardUndefinedServerAttributes.getUndefineAttributeTransformer()));

        rejectExpressions(server, AddressSettingDefinition.PATH, AddressSettingDefinition.REJECTED_EXPRESSION_ATTRIBUTES);

        DiscardUndefinedAttributesTransformer discardBroadcastUndefinedAttributes = new DiscardUndefinedAttributesTransformer(BroadcastGroupDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
        RejectExpressionValuesTransformer rejectBroadcastGroupExpressions = new RejectExpressionValuesTransformer(BroadcastGroupDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        TransformersSubRegistration broadcastGroup = server.registerSubResource(BroadcastGroupDefinition.PATH);
        broadcastGroup.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectBroadcastGroupExpressions,
                discardBroadcastUndefinedAttributes));
        broadcastGroup.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectBroadcastGroupExpressions.getWriteAttributeTransformer(),
                discardBroadcastUndefinedAttributes.getWriteAttributeTransformer()));
        broadcastGroup.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                discardBroadcastUndefinedAttributes.getUndefineAttributeTransformer()));

        DiscardUndefinedAttributesTransformer discardDiscoveryGroupUndefinedAttributes = new DiscardUndefinedAttributesTransformer(DiscoveryGroupDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
        RejectExpressionValuesTransformer rejectDiscoveryGroupExpressions = new RejectExpressionValuesTransformer(DiscoveryGroupDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        TransformersSubRegistration discoveryGroup = server.registerSubResource(DiscoveryGroupDefinition.PATH);
        discoveryGroup.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectDiscoveryGroupExpressions,
                discardDiscoveryGroupUndefinedAttributes));
        discoveryGroup.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectDiscoveryGroupExpressions.getWriteAttributeTransformer(),
                discardDiscoveryGroupUndefinedAttributes.getWriteAttributeTransformer()));
        discoveryGroup.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                discardDiscoveryGroupUndefinedAttributes.getUndefineAttributeTransformer()));

        rejectExpressions(server, DivertDefinition.PATH, DivertDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);

        rejectExpressions(server, BridgeDefinition.PATH, BridgeDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);

        rejectExpressions(server, QueueDefinition.PATH, QueueDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);

        for (String path : new String[]{CommonAttributes.ACCEPTOR, CommonAttributes.CONNECTOR}) {
            TransformersSubRegistration transport = server.registerSubResource(PathElement.pathElement(path));
            rejectExpressions(transport, TransportParamDefinition.PATH, TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        }

        for (String path : new String[]{CommonAttributes.IN_VM_ACCEPTOR, CommonAttributes.IN_VM_CONNECTOR}) {
            TransformersSubRegistration transport = rejectExpressions(server, PathElement.pathElement(path), InVMTransportDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
            rejectExpressions(transport, TransportParamDefinition.PATH, TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        }

        for (String path : new String[]{CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.REMOTE_CONNECTOR}) {
            TransformersSubRegistration transport = server.registerSubResource(PathElement.pathElement(path));
            rejectExpressions(transport, TransportParamDefinition.PATH, TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        }

        for (final String path : MessagingPathHandlers.PATHS.keySet()) {
            rejectExpressions(server, PathElement.pathElement(PATH, path), PATH);
        }

        RejectExpressionValuesTransformer rejectClusterConnectionExpressions = new RejectExpressionValuesTransformer(ClusterConnectionDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        DiscardUndefinedAttributesTransformer discardClusterConnectionUndefinedAttributes = new DiscardUndefinedAttributesTransformer(ClusterConnectionDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
        TransformersSubRegistration clusterConnection = server.registerSubResource(ClusterConnectionDefinition.PATH, rejectClusterConnectionExpressions, rejectClusterConnectionExpressions);
        clusterConnection.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectClusterConnectionExpressions,
                discardClusterConnectionUndefinedAttributes));
        clusterConnection.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectClusterConnectionExpressions.getWriteAttributeTransformer(),
                discardClusterConnectionUndefinedAttributes.getWriteAttributeTransformer()));
        clusterConnection.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                discardClusterConnectionUndefinedAttributes.getUndefineAttributeTransformer()));

        TransformersSubRegistration connectorService = server.registerSubResource(ConnectorServiceDefinition.PATH);
        rejectExpressions(connectorService, ConnectorServiceParamDefinition.PATH, VALUE);

        RejectExpressionValuesTransformer rejectConnectionFactoryExpressions = new RejectExpressionValuesTransformer(ConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        DiscardUndefinedAttributesTransformer discardConnectionFactoryUndefinedAttributes = new DiscardUndefinedAttributesTransformer(ConnectionFactoryDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
        TransformersSubRegistration connectionFactory = server.registerSubResource(ConnectionFactoryDefinition.PATH, rejectConnectionFactoryExpressions, rejectConnectionFactoryExpressions);
        connectionFactory.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                rejectConnectionFactoryExpressions,
                discardConnectionFactoryUndefinedAttributes));
        connectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectConnectionFactoryExpressions.getWriteAttributeTransformer(),
                discardConnectionFactoryUndefinedAttributes.getWriteAttributeTransformer()));
        connectionFactory.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                discardConnectionFactoryUndefinedAttributes.getUndefineAttributeTransformer()));

        RejectExpressionValuesTransformer rejectPooledConnectionFactoryExpressions = new RejectExpressionValuesTransformer(PooledConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);
        DiscardUndefinedAttributesTransformer discardUndefinedPooledConnectionFactoryAttributes = new DiscardUndefinedAttributesTransformer(PooledConnectionFactoryDefinition.NEW_ATTRIBUTES_ADDED_AFTER_1_1_0);
        TransformersSubRegistration pooledConnectionFactory = server.registerSubResource(PooledConnectionFactoryDefinition.PATH);
        pooledConnectionFactory.registerOperationTransformer(ADD, new ChainedOperationTransformer(
                new OperationTransformers.InsertDefaultValuesOperationTransformer(ConnectionFactoryAttributes.Pooled.RECONNECT_ATTEMPTS),
                rejectConnectionFactoryExpressions,
                discardUndefinedPooledConnectionFactoryAttributes));
        pooledConnectionFactory.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                rejectPooledConnectionFactoryExpressions.getWriteAttributeTransformer(),
                discardUndefinedPooledConnectionFactoryAttributes.getWriteAttributeTransformer()));
        pooledConnectionFactory.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, new ChainedOperationTransformer(
                discardUndefinedPooledConnectionFactoryAttributes.getUndefineAttributeTransformer()));

        rejectExpressions(server, GroupingHandlerDefinition.PATH, GroupingHandlerDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);

        rejectExpressions(server, JMSQueueDefinition.PATH, JMSQueueDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);

        rejectExpressions(server, JMSTopicDefinition.PATH, JMSTopicDefinition.ATTRIBUTES_WITH_EXPRESSION_AFTER_1_1_0);

        transformers.registerSubResource(JMSBridgeDefinition.PATH, true);
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
}
