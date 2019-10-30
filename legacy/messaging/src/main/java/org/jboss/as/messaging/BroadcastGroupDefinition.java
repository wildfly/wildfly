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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * Broadcast group resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class BroadcastGroupDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.BROADCAST_GROUP);

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(CONNECTORS, STRING)
            .setRequired(false)
            .setElementValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
            // disallow expressions since the attribute references other configuration items
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition BROADCAST_PERIOD = create("broadcast-period", LONG)
            .setDefaultValue(new ModelNode(2000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { JGROUPS_STACK, JGROUPS_CHANNEL, SOCKET_BINDING, LOCAL_BIND_ADDRESS, LOCAL_BIND_PORT,
        GROUP_ADDRESS, GROUP_PORT, BROADCAST_PERIOD, CONNECTOR_REFS };

    private static final OperationValidator VALIDATOR = new OperationValidator.AttributeDefinitionOperationValidator(ATTRIBUTES);

    static final BroadcastGroupDefinition INSTANCE = new BroadcastGroupDefinition();

    public BroadcastGroupDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.BROADCAST_GROUP),
                new ModelOnlyAddStepHandler(ATTRIBUTES) {

                    @Override
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                        model.setEmptyObject();

                        VALIDATOR.validateAndSet(operation, model);
                    }

                    @Override
                    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                        super.populateModel(context, operation, resource);

                        final ModelNode connectorRefs = resource.getModel().get(CONNECTOR_REFS.getName());
                        if (connectorRefs.isDefined()) {
                            context.addStep(new OperationStepHandler() {
                                @Override
                                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                    validateConnectors(context, operation, connectorRefs);
                                }
                            }, OperationContext.Stage.MODEL);
                        }

                    }
                },
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
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
        PathAddress hornetqServer = context.getCurrentAddress().getParent();
        Resource hornetQServerResource = context.readResourceFromRoot(hornetqServer, false);
        Set<String> availableConnectors = new HashSet<String>();
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.CONNECTOR));
        return availableConnectors;
    }
}
