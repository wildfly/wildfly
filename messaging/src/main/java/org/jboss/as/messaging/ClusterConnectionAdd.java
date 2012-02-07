/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hornetq.core.config.ClusterConnectionConfiguration;
import org.hornetq.core.config.Configuration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for adding a cluster connection.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ClusterConnectionAdd extends AbstractAddStepHandler implements DescriptionProvider {

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = org.jboss.as.controller.operations.common.Util.getOperation(ADD, address, subModel);

        return operation;
    }

    public static final ClusterConnectionAdd INSTANCE = new ClusterConnectionAdd();

    private ClusterConnectionAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        model.setEmptyObject();

        boolean hasStatic = operation.hasDefined(ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS.getName());
        boolean hasDiscGroup = operation.hasDefined(CommonAttributes.DISCOVERY_GROUP_NAME.getName());
        if (hasStatic && hasDiscGroup) {
            throw new OperationFailedException(new ModelNode().set(String.format("Operation cannot include both parameter %s and parameter %s",
                    ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS.getName(), CommonAttributes.DISCOVERY_GROUP_NAME.getName())));
        }

        for (final AttributeDefinition attributeDefinition : CommonAttributes.CLUSTER_CONNECTION_ATTRIBUTES) {
            if (hasDiscGroup && attributeDefinition == ConnectorRefsAttribute.CLUSTER_CONNECTION_CONNECTORS) {
                continue;
            } else if (hasStatic && attributeDefinition == CommonAttributes.DISCOVERY_GROUP_NAME) {
                continue;
            }
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(false);
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = registry.getService(hqServiceName);
        if (hqService != null) {
            context.reloadRequired();
        }
        // else MessagingSubsystemAdd will add a handler that calls addBroadcastGroupConfigs
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getClusterConnectionAdd(locale);
    }

    static void addClusterConnectionConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.CLUSTER_CONNECTION)) {
            final List<ClusterConnectionConfiguration> configs = configuration.getClusterConfigurations();
            for (Property prop : model.get(CommonAttributes.CLUSTER_CONNECTION).asPropertyList()) {
                configs.add(createClusterConnectionConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    static ClusterConnectionConfiguration createClusterConnectionConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final String address = CommonAttributes.CLUSTER_CONNECTION_ADDRESS.resolveModelAttribute(context, model).asString();
        final String connectorName = CommonAttributes.CONNECTOR_REF.resolveModelAttribute(context, model).asString();
        final long retryInterval = CommonAttributes.CLUSTER_CONNECTION_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final boolean duplicateDetection = CommonAttributes.CLUSTER_CONNECTION_USE_DUPLICATE_DETECTION.resolveModelAttribute(context, model).asBoolean();
        final boolean forwardWhenNoConsumers = CommonAttributes.FORWARD_WHEN_NO_CONSUMERS.resolveModelAttribute(context, model).asBoolean();
        final int maxHops = CommonAttributes.MAX_HOPS.resolveModelAttribute(context, model).asInt();
        final int confirmationWindowSize = CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt();
        final ModelNode discoveryNode = CommonAttributes.DISCOVERY_GROUP_NAME.resolveModelAttribute(context, model);
        final String discoveryGroupName = discoveryNode.isDefined() ? discoveryNode.asString() : null;
        final List<String> staticConnectors = discoveryGroupName == null ? getStaticConnectors(model) : null;
        final boolean allowDirectOnly = CommonAttributes.ALLOW_DIRECT_CONNECTIONS_ONLY.resolveModelAttribute(context, model).asBoolean();

        if (discoveryGroupName != null) {
            return new ClusterConnectionConfiguration(name, address, connectorName, retryInterval, duplicateDetection,
                    forwardWhenNoConsumers, maxHops, confirmationWindowSize, discoveryGroupName);
        } else {
            return new ClusterConnectionConfiguration(name, address, connectorName, retryInterval, duplicateDetection,
                    forwardWhenNoConsumers, maxHops, confirmationWindowSize, staticConnectors, allowDirectOnly);
        }
    }

    private static List<String> getStaticConnectors(ModelNode model) {
        if (!model.hasDefined(CommonAttributes.STATIC_CONNECTORS))
            return null;

        List<String> result = new ArrayList<String>();
        for (ModelNode connector : model.require(CommonAttributes.STATIC_CONNECTORS).asList()) {
            result.add(connector.asString());
        }
        return result;
    }
}
