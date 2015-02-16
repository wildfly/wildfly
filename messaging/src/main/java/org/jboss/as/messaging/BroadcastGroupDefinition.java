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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;
import static org.jboss.as.messaging.CommonAttributes.CONNECTORS;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.GROUP_PORT;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_PORT;
import static org.jboss.as.messaging.CommonAttributes.SOCKET_BINDING;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * Broadcast group resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class BroadcastGroupDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.BROADCAST_GROUP);

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(CONNECTORS, STRING)
            .setAllowNull(true)
            .setElementValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
            // disallow expressions since the attribute references other configuration items
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition BROADCAST_PERIOD = create("broadcast-period", LONG)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.getDefaultBroadcastPeriod()))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { JGROUPS_STACK, JGROUPS_CHANNEL, SOCKET_BINDING, LOCAL_BIND_ADDRESS, LOCAL_BIND_PORT,
        GROUP_ADDRESS, GROUP_PORT, BROADCAST_PERIOD, CONNECTOR_REFS };

    public static final String GET_CONNECTOR_PAIRS_AS_JSON = "get-connector-pairs-as-json";

    private final boolean registerRuntimeOnly;

    public BroadcastGroupDefinition(boolean registerRuntimeOnly) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BROADCAST_GROUP),
                BroadcastGroupAdd.INSTANCE,
                BroadcastGroupRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, BroadcastGroupWriteAttributeHandler.INSTANCE);
            }
        }

        if (registerRuntimeOnly) {
            BroadcastGroupControlHandler.INSTANCE.registerAttributes(registry);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (registerRuntimeOnly) {
            BroadcastGroupControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());

            SimpleOperationDefinition op = new SimpleOperationDefinitionBuilder(GET_CONNECTOR_PAIRS_AS_JSON, getResourceDescriptionResolver())
                .withFlags(EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY))
                .setReplyType(STRING)
                .build();
            registry.registerOperationHandler(op, BroadcastGroupControlHandler.INSTANCE);
        }
        super.registerOperations(registry);
    }

    static void validateConnectors(OperationContext context, ModelNode operation, ModelNode connectorRefs) throws OperationFailedException {
        final Set<String> availableConnectors =  getAvailableConnectors(context,operation);
        final List<ModelNode> operationAddress = operation.get(ModelDescriptionConstants.ADDRESS).asList();
        final String broadCastGroup = operationAddress.get(operationAddress.size()-1).get(CommonAttributes.BROADCAST_GROUP).asString();
        for(ModelNode connectorRef:connectorRefs.asList()){
            final String connectorName = connectorRef.asString();
            if(!availableConnectors.contains(connectorName)){
                throw MessagingLogger.ROOT_LOGGER.wrongConnectorRefInBroadCastGroup(broadCastGroup, connectorName, availableConnectors);
            }
        }
    }

    private static Set<String> getAvailableConnectors(final OperationContext context,final ModelNode operation) throws OperationFailedException{
        PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        PathAddress hornetqServer = MessagingServices.getHornetQServerPathAddress(address);
        Resource hornetQServerResource = context.readResourceFromRoot(hornetqServer);
        Set<String> availableConnectors = new HashSet<String>();
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.CONNECTOR));
        return availableConnectors;
    }
}
