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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTORS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SOCKET_BINDING;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.extension.messaging.activemq.shallow.ShallowResourceDefinition;

/**
 * Broadcast group resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 * @deprecated Use JGroupsBroadcastGroupDefinition or SocketBroadcastGroupDefinition.
 */
public class BroadcastGroupDefinition extends ShallowResourceDefinition {
    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = new StringListAttributeDefinition.Builder(CONNECTORS)
            .setRequired(false)
            .setElementValidator(new StringLengthValidator(1))
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

     /**
     * @see ActiveMQDefaultConfiguration#getDefaultBroadcastPeriod
     */
    public static final SimpleAttributeDefinition BROADCAST_PERIOD = create("broadcast-period", LONG)
            .setDefaultValue(new ModelNode(2000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    public static final SimpleAttributeDefinition JGROUPS_CHANNEL_FACTORY = create(CommonAttributes.JGROUPS_CHANNEL_FACTORY)
            .build();

    public static final SimpleAttributeDefinition JGROUPS_CHANNEL = create(CommonAttributes.JGROUPS_CHANNEL)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { JGROUPS_CHANNEL_FACTORY, JGROUPS_CHANNEL, JGROUPS_CLUSTER, SOCKET_BINDING,
            BROADCAST_PERIOD, CONNECTOR_REFS };

    public static final String GET_CONNECTOR_PAIRS_AS_JSON = "get-connector-pairs-as-json";

    BroadcastGroupDefinition(boolean registerRuntimeOnly) {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.BROADCAST_GROUP_PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BROADCAST_GROUP))
                .setAddHandler(BroadcastGroupAdd.INSTANCE)
                .setRemoveHandler(BroadcastGroupRemove.INSTANCE)
                .setFeature(false)
                .setDeprecationData(new DeprecationData(MessagingExtension.VERSION_9_0_0, true)),
                registerRuntimeOnly);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }


    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            BroadcastGroupControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());

            SimpleOperationDefinition op = new SimpleOperationDefinitionBuilder(GET_CONNECTOR_PAIRS_AS_JSON, getResourceDescriptionResolver())
                    .setReadOnly()
                    .setRuntimeOnly()
                    .setReplyType(STRING)
                    .build();
            registry.registerOperationHandler(op, BroadcastGroupControlHandler.INSTANCE);
        }
    }

    static void validateConnectors(OperationContext context, ModelNode operation, ModelNode connectorRefs) throws OperationFailedException {
        final Set<String> availableConnectors =  getAvailableConnectors(context,operation);
        final List<ModelNode> operationAddress = operation.get(ModelDescriptionConstants.ADDRESS).asList();
        if(connectorRefs.isDefined()) {
            for(ModelNode connectorRef:connectorRefs.asList()){
                final String connectorName = connectorRef.asString();
                if(!availableConnectors.contains(connectorName)){
                    throw MessagingLogger.ROOT_LOGGER.wrongConnectorRefInBroadCastGroup(getBroadCastGroupName(operationAddress.get(operationAddress.size()-1)), connectorName, availableConnectors);
                }
            }
        }
    }

    private static String getBroadCastGroupName(ModelNode node) {
        if(node.hasDefined(CommonAttributes.BROADCAST_GROUP)) {
            return node.get(CommonAttributes.BROADCAST_GROUP).asString();
        }
        if(node.hasDefined(CommonAttributes.SOCKET_BROADCAST_GROUP)) {
            return node.get(CommonAttributes.SOCKET_BROADCAST_GROUP).asString();
        }
        if(node.hasDefined(CommonAttributes.JGROUPS_BROADCAST_GROUP)) {
            return node.get(CommonAttributes.JGROUPS_BROADCAST_GROUP).asString();
        }
        return ModelType.UNDEFINED.name();
    }

    // FIXME use capabilities & requirements
    private static Set<String> getAvailableConnectors(final OperationContext context,final ModelNode operation) throws OperationFailedException{
        PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        PathAddress active = MessagingServices.getActiveMQServerPathAddress(address);
        Set<String> availableConnectors = new HashSet<>();

        Resource subsystemResource = context.readResourceFromRoot(active.getParent(), false);
        availableConnectors.addAll(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));

        Resource activeMQServerResource = context.readResourceFromRoot(active, false);
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR));
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR));
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.CONNECTOR));
        return availableConnectors;
    }

    @Override
    public PathAddress convert(OperationContext context, ModelNode operation) {
        PathAddress parent = context.getCurrentAddress().getParent();
        PathAddress targetAddress = parent.append(CommonAttributes.JGROUPS_BROADCAST_GROUP, context.getCurrentAddressValue());
        try {
            context.readResourceFromRoot(targetAddress, false);
            return targetAddress;
        } catch (Resource.NoSuchResourceException ex) {
            return parent.append(CommonAttributes.SOCKET_BROADCAST_GROUP, context.getCurrentAddressValue());
        }
    }

    @Override
    public Set<String> getIgnoredAttributes(OperationContext context, ModelNode operation) {
        PathAddress targetAddress = context.getCurrentAddress().getParent().append(CommonAttributes.JGROUPS_BROADCAST_GROUP, context.getCurrentAddressValue());
        Set<String> ignoredAttributes = new HashSet<>();
        try {
            context.readResourceFromRoot(targetAddress, false);
            ignoredAttributes.add(SOCKET_BINDING.getName());
        } catch (Resource.NoSuchResourceException ex) {
            ignoredAttributes.add(JGROUPS_CHANNEL_FACTORY.getName());
            ignoredAttributes.add(JGROUPS_CHANNEL.getName());
            ignoredAttributes.add(JGROUPS_CLUSTER.getName());
        }
        return ignoredAttributes;
    }
}
