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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.BridgeDefinition.ATTRIBUTES;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.DISCOVERY_GROUP_NAME;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.FORWARDING_ADDRESS;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.INITIAL_CONNECT_ATTEMPTS;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.PASSWORD;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.PRODUCER_WINDOW_SIZE;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.QUEUE_NAME;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.RECONNECT_ATTEMPTS;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.RECONNECT_ATTEMPTS_ON_SAME_NODE;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.USER;
import static org.wildfly.extension.messaging.activemq.BridgeDefinition.USE_DUPLICATE_DETECTION;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.TransformerConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Handler for adding a bridge.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BridgeAdd extends AbstractAddStepHandler {

    public static final BridgeAdd INSTANCE = new BridgeAdd();

    private BridgeAdd() {}

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        model.setEmptyObject();

        for (final AttributeDefinition attributeDefinition : ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = registry.getService(serviceName);
        if (service != null) {

            // The original subsystem initialization is complete; use the control object to create the divert
            if (service.getState() != ServiceController.State.UP) {
                throw MessagingLogger.ROOT_LOGGER.invalidServiceState(serviceName, ServiceController.State.UP, service.getState());
            }

            final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

            BridgeConfiguration bridgeConfig = createBridgeConfiguration(context, name, model);

            ActiveMQServerControl serverControl = ActiveMQServer.class.cast(service.getValue()).getActiveMQServerControl();
            createBridge(name, bridgeConfig, serverControl);

        }
        // else the initial subsystem install is not complete; MessagingSubsystemAdd will add a
        // handler that calls addBridgeConfigs
    }

    static void addBridgeConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.BRIDGE)) {
            final List<BridgeConfiguration> configs = configuration.getBridgeConfigurations();
            for (Property prop : model.get(CommonAttributes.BRIDGE).asPropertyList()) {
                configs.add(createBridgeConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    static BridgeConfiguration createBridgeConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final String queueName = QUEUE_NAME.resolveModelAttribute(context, model).asString();
        final ModelNode forwardingNode = FORWARDING_ADDRESS.resolveModelAttribute(context, model);
        final String forwardingAddress = forwardingNode.isDefined() ? forwardingNode.asString() : null;
        final ModelNode filterNode = CommonAttributes.FILTER.resolveModelAttribute(context, model);
        final String filterString = filterNode.isDefined() ? filterNode.asString() : null;
        final int minLargeMessageSize = CommonAttributes.MIN_LARGE_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt();
        final long retryInterval = CommonAttributes.RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final double retryIntervalMultiplier = CommonAttributes.RETRY_INTERVAL_MULTIPLIER.resolveModelAttribute(context, model).asDouble();
        final long maxRetryInterval = CommonAttributes.MAX_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final int initialConnectAttempts = INITIAL_CONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt();
        final int reconnectAttempts = RECONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt();
        final int reconnectAttemptsOnSameNode = RECONNECT_ATTEMPTS_ON_SAME_NODE.resolveModelAttribute(context, model).asInt();
        final boolean useDuplicateDetection = USE_DUPLICATE_DETECTION.resolveModelAttribute(context, model).asBoolean();
        final int confirmationWindowSize = CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt();
        final int producerWindowSize = PRODUCER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt();
        final long clientFailureCheckPeriod = CommonAttributes.CHECK_PERIOD.resolveModelAttribute(context, model).asLong();
        final long connectionTTL = CommonAttributes.CONNECTION_TTL.resolveModelAttribute(context, model).asLong();
        final ModelNode discoveryNode = DISCOVERY_GROUP_NAME.resolveModelAttribute(context, model);
        final String discoveryGroupName = discoveryNode.isDefined() ? discoveryNode.asString() : null;
        List<String> staticConnectors = discoveryGroupName == null ? getStaticConnectors(model) : null;
        final boolean ha = CommonAttributes.HA.resolveModelAttribute(context, model).asBoolean();
        final String user = USER.resolveModelAttribute(context, model).asString();
        final String password = PASSWORD.resolveModelAttribute(context, model).asString();

        BridgeConfiguration config = new BridgeConfiguration()
                .setName(name)
                .setQueueName(queueName)
                .setForwardingAddress(forwardingAddress)
                .setFilterString(filterString)
                .setMinLargeMessageSize(minLargeMessageSize)
                .setClientFailureCheckPeriod(clientFailureCheckPeriod)
                .setConnectionTTL(connectionTTL)
                .setRetryInterval(retryInterval)
                .setMaxRetryInterval(maxRetryInterval)
                .setRetryIntervalMultiplier(retryIntervalMultiplier)
                .setInitialConnectAttempts(initialConnectAttempts)
                .setReconnectAttempts(reconnectAttempts)
                .setReconnectAttemptsOnSameNode(reconnectAttemptsOnSameNode)
                .setUseDuplicateDetection(useDuplicateDetection)
                .setConfirmationWindowSize(confirmationWindowSize)
                .setProducerWindowSize(producerWindowSize)
                .setHA(ha)
                .setUser(user)
                .setPassword(password);

        if (discoveryGroupName != null) {
            config.setDiscoveryGroupName(discoveryGroupName);
        } else {
            config.setStaticConnectors(staticConnectors);
        }
        final ModelNode transformerClassName = CommonAttributes.TRANSFORMER_CLASS_NAME.resolveModelAttribute(context, model);
        if (transformerClassName.isDefined()) {
            config.setTransformerConfiguration(new TransformerConfiguration(transformerClassName.asString()));
        }

        return config;
    }

    private static List<String> getStaticConnectors(ModelNode model) {
        List<String> result = new ArrayList<String>();
        for (ModelNode connector : model.require(CommonAttributes.STATIC_CONNECTORS).asList()) {
            result.add(connector.asString());
        }
        return result;
    }

    static void createBridge(String name, BridgeConfiguration bridgeConfig, ActiveMQServerControl serverControl) {
        try {
            String transformerClassName = bridgeConfig.getTransformerConfiguration() != null ? bridgeConfig.getTransformerConfiguration().getClassName() : null;
            if (bridgeConfig.getDiscoveryGroupName() != null) {
                serverControl.createBridge(name, bridgeConfig.getQueueName(), bridgeConfig.getForwardingAddress(),
                        bridgeConfig.getFilterString(), transformerClassName, bridgeConfig.getRetryInterval(),
                        bridgeConfig.getRetryIntervalMultiplier(), bridgeConfig.getInitialConnectAttempts(),
                        bridgeConfig.getReconnectAttempts(), bridgeConfig.isUseDuplicateDetection(),
                        bridgeConfig.getConfirmationWindowSize(), bridgeConfig.getClientFailureCheckPeriod(),
                        bridgeConfig.getDiscoveryGroupName(), true, bridgeConfig.isHA(), bridgeConfig.getUser(),
                        bridgeConfig.getPassword());
            } else {
                boolean first = true;
                String connectors = "";
                for (String connector : bridgeConfig.getStaticConnectors()) {
                    if (!first) {
                        connectors += ",";
                    } else {
                        first = false;
                    }
                    connectors += connector;
                }
                serverControl.createBridge(name, bridgeConfig.getQueueName(), bridgeConfig.getForwardingAddress(),
                        bridgeConfig.getFilterString(), transformerClassName, bridgeConfig.getRetryInterval(),
                        bridgeConfig.getRetryIntervalMultiplier(), bridgeConfig.getInitialConnectAttempts(),
                        bridgeConfig.getReconnectAttempts(), bridgeConfig.isUseDuplicateDetection(),
                        bridgeConfig.getConfirmationWindowSize(), bridgeConfig.getClientFailureCheckPeriod(),
                        connectors, false, bridgeConfig.isHA(), bridgeConfig.getUser(),
                        bridgeConfig.getPassword());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TODO should this be an OFE instead?
            throw new RuntimeException(e);
        }
    }
}
