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
import static org.jboss.as.controller.transform.OperationResultTransformer.ORIGINAL_RESULT;
import static org.jboss.as.controller.transform.description.DiscardAttributeChecker.UNDEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.jboss.as.controller.transform.description.RejectAttributeChecker.SIMPLE_EXPRESSIONS;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.RUNTIME_QUEUE;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_1_0;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttributes.*;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;
import org.jboss.dmr.ModelNode;

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

        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        // discard JMS bridge resources added in 1.2.0
        subsystemRoot.discardChildResource(JMSBridgeDefinition.PATH);
        // discard runtime resources
        subsystemRoot.discardChildResource(CoreAddressDefinition.PATH);
        subsystemRoot.discardChildResource(PathElement.pathElement(RUNTIME_QUEUE));

        ResourceTransformationDescriptionBuilder hornetqServer = subsystemRoot.addChildResource(PathElement.pathElement(HORNETQ_SERVER))
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, HornetQServerResourceDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                        .addRejectCheck(DEFINED, HornetQServerResourceDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .setDiscard(UNDEFINED, HornetQServerResourceDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                .end()
                .addOperationTransformationOverride(ADD)
                        .inheritResourceAttributeDefinitions()
                        // add default value for id-cache-size & clustered
                        .setCustomOperationTransformer(new OperationTransformer() {
                            @Override
                            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                                operation.get(ID_CACHE_SIZE.getName()).set(ID_CACHE_SIZE.getDefaultValue());
                                operation.get(CLUSTERED.getName()).set(CLUSTERED.getDefaultValue());
                                return new TransformedOperation(operation, ORIGINAL_RESULT);
                            }
                        })
                .end();

        for (String path : MessagingPathHandlers.PATHS.keySet()) {
            hornetqServer.addChildResource(PathElement.pathElement(PATH, path))
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, PATH)
                    .end();
        }

        for (String path : new String[]{CommonAttributes.IN_VM_ACCEPTOR, CommonAttributes.IN_VM_CONNECTOR}) {
            final ResourceTransformationDescriptionBuilder transport = hornetqServer.addChildResource(PathElement.pathElement(path))
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, InVMTransportDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                    .end()
                    .addOperationTransformationOverride(ADD)
                            .inheritResourceAttributeDefinitions()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, CommonAttributes.PARAM) // additional attribute to the ADD operation
                    .end();
            transport.addChildResource(TransportParamDefinition.PATH)
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                    .end();
        }

        for (String path : new String[]{CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.REMOTE_CONNECTOR, CommonAttributes.ACCEPTOR, CommonAttributes.CONNECTOR}) {
            final ResourceTransformationDescriptionBuilder transport = hornetqServer.addChildResource(PathElement.pathElement(path))
                    .addOperationTransformationOverride(ADD)
                            .inheritResourceAttributeDefinitions()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, CommonAttributes.PARAM) // additional attribute to the ADD operation
                    .end();
            transport.addChildResource(TransportParamDefinition.PATH)
                    .getAttributeBuilder()
                            .addRejectCheck(SIMPLE_EXPRESSIONS, TransportParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                    .end();
        }

        hornetqServer.addChildResource(BroadcastGroupDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, BroadcastGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, BroadcastGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, BroadcastGroupDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(DiscoveryGroupDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, DiscoveryGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, DiscoveryGroupDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, DiscoveryGroupDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(DivertDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, DivertDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();


        hornetqServer.addChildResource(QueueDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, QueueDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(BridgeDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, BridgeDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(ClusterConnectionDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, ClusterConnectionDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, ClusterConnectionDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, ClusterConnectionDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(GroupingHandlerDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, GroupingHandlerDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(AddressSettingDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, AddressSettingDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        ResourceTransformationDescriptionBuilder connectorService = hornetqServer.addChildResource(ConnectorServiceDefinition.PATH);

        connectorService.addChildResource(ConnectorServiceParamDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, ConnectorServiceParamDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(ConnectionFactoryDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, ConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, ConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, ConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(PooledConnectionFactoryDefinition.PATH)
                .getAttributeBuilder()
                        .setDiscard(UNDEFINED, PooledConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(DEFINED, PooledConnectionFactoryDefinition.ATTRIBUTES_ADDED_IN_1_2_0)
                        .addRejectCheck(SIMPLE_EXPRESSIONS, PooledConnectionFactoryDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end()
                .addOperationTransformationOverride(ADD)
                        .inheritResourceAttributeDefinitions()
                        // add default value for reconnect-attempts
                        .setCustomOperationTransformer(new OperationTransformer() {
                            @Override
                            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
                                operation.get(Pooled.RECONNECT_ATTEMPTS.getName()).set(Pooled.RECONNECT_ATTEMPTS.getDefaultValue());
                                return new TransformedOperation(operation, ORIGINAL_RESULT);
                            }
                        })
                .end();

        hornetqServer.addChildResource(JMSQueueDefinition.PATH)
                .getAttributeBuilder()
                        .addRejectCheck(SIMPLE_EXPRESSIONS, JMSQueueDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        hornetqServer.addChildResource(JMSTopicDefinition.PATH)
                .getAttributeBuilder()
                .addRejectCheck(SIMPLE_EXPRESSIONS, JMSTopicDefinition.ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0)
                .end();

        TransformationDescription.Tools.register(subsystemRoot.build(), subsystem, VERSION_1_1_0);
    }

    private static String[] concat(AttributeDefinition[] attrs1, String... attrs2) {
        String[] newAttrs = new String[attrs1.length + attrs2.length];
        for(int i = 0; i < attrs1.length; i++) {
            newAttrs[i] = attrs1[i].getName();
        }
        for(int i = 0; i < attrs2.length; i++) {
            newAttrs[attrs1.length + i] = attrs2[i];
        }
        return newAttrs;
    }
}
