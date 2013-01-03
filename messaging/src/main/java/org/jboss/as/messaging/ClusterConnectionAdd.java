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

import static org.jboss.as.messaging.ClusterConnectionDefinition.CONNECTOR_REFS;
import static org.jboss.as.messaging.ClusterConnectionDefinition.DISCOVERY_GROUP_NAME;

import java.util.ArrayList;
import java.util.List;

import org.hornetq.core.config.ClusterConnectionConfiguration;
import org.hornetq.core.config.Configuration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
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
public class ClusterConnectionAdd extends AbstractAddStepHandler {

    public static final ClusterConnectionAdd INSTANCE = new ClusterConnectionAdd();

    private ClusterConnectionAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        model.setEmptyObject();

        AlternativeAttributeCheckHandler.checkAlternatives(operation, CONNECTOR_REFS.getName(), (DISCOVERY_GROUP_NAME.getName()), true);

        for (final AttributeDefinition attributeDefinition : ClusterConnectionDefinition.ATTRIBUTES) {
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

    static void addClusterConnectionConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.CLUSTER_CONNECTION)) {
            final List<ClusterConnectionConfiguration> configs = configuration.getClusterConfigurations();
            for (Property prop : model.get(CommonAttributes.CLUSTER_CONNECTION).asPropertyList()) {
                configs.add(createClusterConnectionConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    static ClusterConnectionConfiguration createClusterConnectionConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final String address = ClusterConnectionDefinition.ADDRESS.resolveModelAttribute(context, model).asString();
        final String connectorName = ClusterConnectionDefinition.CONNECTOR_REF.resolveModelAttribute(context, model).asString();
        final long retryInterval = ClusterConnectionDefinition.RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final boolean duplicateDetection = ClusterConnectionDefinition.USE_DUPLICATE_DETECTION.resolveModelAttribute(context, model).asBoolean();
        final long connectionTTL = ClusterConnectionDefinition.CONNECTION_TTL.resolveModelAttribute(context, model).asInt();
        final int reconnectAttempts = ClusterConnectionDefinition.RECONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt();
        final long maxRetryInterval = ClusterConnectionDefinition.MAX_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final double retryIntervalMultiplier = ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER.resolveModelAttribute(context, model).asDouble();
        final long clientFailureCheckPeriod = ClusterConnectionDefinition.CHECK_PERIOD.resolveModelAttribute(context, model).asInt();

        final boolean forwardWhenNoConsumers = ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS.resolveModelAttribute(context, model).asBoolean();
        final int maxHops = ClusterConnectionDefinition.MAX_HOPS.resolveModelAttribute(context, model).asInt();
        final int confirmationWindowSize = CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt();
        final ModelNode discoveryNode = ClusterConnectionDefinition.DISCOVERY_GROUP_NAME.resolveModelAttribute(context, model);
        final String discoveryGroupName = discoveryNode.isDefined() ? discoveryNode.asString() : null;
        final List<String> staticConnectors = discoveryGroupName == null ? getStaticConnectors(model) : null;
        final boolean allowDirectOnly = ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY.resolveModelAttribute(context, model).asBoolean();
        final int minLargeMessageSize = CommonAttributes.MIN_LARGE_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt();
        final long callTimeout = CommonAttributes.CALL_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final long callFailoverTimeout = CommonAttributes.CALL_FAILOVER_TIMEOUT.resolveModelAttribute(context, model).asLong();

        if (discoveryGroupName != null) {
            return new ClusterConnectionConfiguration(name, address, connectorName, minLargeMessageSize, clientFailureCheckPeriod, connectionTTL,
                    retryInterval, retryIntervalMultiplier, maxRetryInterval, reconnectAttempts, callTimeout, callFailoverTimeout,
                    duplicateDetection, forwardWhenNoConsumers, maxHops, confirmationWindowSize,
                    discoveryGroupName);
        } else {
            return new ClusterConnectionConfiguration(name, address, connectorName, minLargeMessageSize, clientFailureCheckPeriod, connectionTTL,
                    retryInterval, retryIntervalMultiplier, maxRetryInterval, reconnectAttempts, callTimeout, callFailoverTimeout,
                    duplicateDetection, forwardWhenNoConsumers, maxHops, confirmationWindowSize,
                    staticConnectors, allowDirectOnly);
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
