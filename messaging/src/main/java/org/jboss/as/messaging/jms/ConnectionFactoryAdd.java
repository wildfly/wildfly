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

package org.jboss.as.messaging.jms;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.AUTO_GROUP;
import static org.jboss.as.messaging.CommonAttributes.BLOCK_ON_ACK;
import static org.jboss.as.messaging.CommonAttributes.BLOCK_ON_DURABLE_SEND;
import static org.jboss.as.messaging.CommonAttributes.BLOCK_ON_NON_DURABLE_SEND;
import static org.jboss.as.messaging.CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT;
import static org.jboss.as.messaging.CommonAttributes.CALL_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.COMPRESS_LARGE_MESSAGES;
import static org.jboss.as.messaging.CommonAttributes.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_MAX_RATE;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.DUPS_OK_BATCH_SIZE;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ID;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.LOAD_BALANCING_CLASS_NAME;
import static org.jboss.as.messaging.CommonAttributes.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.PRE_ACK;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY_RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.CommonAttributes.USE_GLOBAL_POOLS;

import java.util.ArrayList;
import java.util.List;

import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * Update adding a connection factory to the subsystem. The
 * runtime action will create the {@link ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class ConnectionFactoryAdd extends AbstractAddStepHandler {

    public static final ConnectionFactoryAdd INSTANCE = new ConnectionFactoryAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attribute : JMSServices.CONNECTION_FACTORY_ATTRS) {
            attribute.validateAndSet(operation, model);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        final ConnectionFactoryConfiguration configuration = createConfiguration(name, operation);
        final ConnectionFactoryService service = new ConnectionFactoryService(configuration);
        final ServiceName serviceName = JMSServices.getConnectionFactoryBaseServiceName(hqServiceName).append(name);
        newControllers.add(context.getServiceTarget().addService(serviceName, service)
                .addDependency(JMSServices.getJmsManagerBaseServiceName(hqServiceName), JMSServerManager.class, service.getJmsServer())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());
    }

    static ConnectionFactoryConfiguration createConfiguration(final String name, final ModelNode operation) throws OperationFailedException {
        final ConnectionFactoryConfiguration config = new ConnectionFactoryConfigurationImpl(name, HornetQClient.DEFAULT_HA, JndiEntriesAttribute.getJndiBindings(operation));

        config.setHA(HA.validateResolvedOperation(operation).asBoolean());  // TODO this is not in the xsd
        config.setAutoGroup(AUTO_GROUP.validateResolvedOperation(operation).asBoolean());
        config.setBlockOnAcknowledge(BLOCK_ON_ACK.validateResolvedOperation(operation).asBoolean());
        config.setBlockOnDurableSend(BLOCK_ON_DURABLE_SEND.validateResolvedOperation(operation).asBoolean());
        config.setBlockOnNonDurableSend(BLOCK_ON_NON_DURABLE_SEND.validateResolvedOperation(operation).asBoolean());
        config.setCacheLargeMessagesClient(CACHE_LARGE_MESSAGE_CLIENT.validateResolvedOperation(operation).asBoolean());
        config.setCallTimeout(CALL_TIMEOUT.validateResolvedOperation(operation).asLong());
        config.setClientFailureCheckPeriod(CLIENT_FAILURE_CHECK_PERIOD.validateResolvedOperation(operation).asInt());
        final ModelNode clientId = CLIENT_ID.validateResolvedOperation(operation);
        if (clientId.isDefined()) {
            config.setClientID(clientId.asString());
        }
        config.setCompressLargeMessages(COMPRESS_LARGE_MESSAGES.validateResolvedOperation(operation).asBoolean());
        config.setConfirmationWindowSize(CONFIRMATION_WINDOW_SIZE.validateResolvedOperation(operation).asInt());
        config.setConnectionTTL(CONNECTION_TTL.validateResolvedOperation(operation).asLong());
        if (operation.hasDefined(CONNECTOR)) {
            ModelNode connectorRefs = operation.get(CONNECTOR);
            List<String> connectorNames = new ArrayList<String>();
            for (String connectorName : operation.get(CONNECTOR).keys()) {
                ModelNode connectorRef = connectorRefs.get(connectorName);
                connectorNames.add(connectorName);
            }
            config.setConnectorNames(connectorNames);
        }
        //config.setConnectorConfigs(connectorConfigs)
        // config.setConnectorNames(connectors);
        config.setConsumerMaxRate(CONSUMER_MAX_RATE.validateResolvedOperation(operation).asInt());
        config.setConsumerWindowSize(CONSUMER_WINDOW_SIZE.validateResolvedOperation(operation).asInt());
        final ModelNode discoveryGroupName = DISCOVERY_GROUP_NAME.validateResolvedOperation(operation);
        if (discoveryGroupName.isDefined()) {
            config.setDiscoveryGroupName(discoveryGroupName.asString());
        }

        config.setDupsOKBatchSize(DUPS_OK_BATCH_SIZE.validateResolvedOperation(operation).asInt());
        config.setFailoverOnInitialConnection(FAILOVER_ON_INITIAL_CONNECTION.validateResolvedOperation(operation).asBoolean());

        final ModelNode groupId = GROUP_ID.validateResolvedOperation(operation);
        if(groupId.isDefined()) {
            config.setGroupID(groupId.asString());
        }

        final ModelNode lbcn = LOAD_BALANCING_CLASS_NAME.validateResolvedOperation(operation);
        if(lbcn.isDefined()) {
            config.setLoadBalancingPolicyClassName(lbcn.asString());
        }

        config.setMaxRetryInterval(MAX_RETRY_INTERVAL.validateResolvedOperation(operation).asLong());
        config.setMinLargeMessageSize(MIN_LARGE_MESSAGE_SIZE.validateResolvedOperation(operation).asInt());
        config.setPreAcknowledge(PRE_ACK.validateResolvedOperation(operation).asBoolean());
        config.setProducerMaxRate(PRODUCER_MAX_RATE.validateResolvedOperation(operation).asInt());
        config.setProducerWindowSize(PRODUCER_WINDOW_SIZE.validateResolvedOperation(operation).asInt());
        config.setReconnectAttempts(CONNECTION_FACTORY_RECONNECT_ATTEMPTS.validateResolvedOperation(operation).asInt());
        config.setRetryInterval(RETRY_INTERVAL.validateResolvedOperation(operation).asLong());
        config.setRetryIntervalMultiplier(RETRY_INTERVAL_MULTIPLIER.validateResolvedOperation(operation).asDouble());
        config.setScheduledThreadPoolMaxSize(CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE.validateResolvedOperation(operation).asInt());
        config.setThreadPoolMaxSize(CONNECTION_THREAD_POOL_MAX_SIZE.validateResolvedOperation(operation).asInt());
        config.setTransactionBatchSize(TRANSACTION_BATCH_SIZE.validateResolvedOperation(operation).asInt());
        config.setUseGlobalPools(USE_GLOBAL_POOLS.validateResolvedOperation(operation).asBoolean());
        config.setLoadBalancingPolicyClassName(LOAD_BALANCING_CLASS_NAME.validateResolvedOperation(operation).asString());

        return config;
    }

    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        for (final AttributeDefinition attribute : JMSServices.CONNECTION_FACTORY_ATTRS) {
            final String attrName = attribute.getName();
            if (subModel.hasDefined(attrName)) {
                operation.get(attrName).set(subModel.get(attrName));
            }
        }

        return operation;
    }

}
