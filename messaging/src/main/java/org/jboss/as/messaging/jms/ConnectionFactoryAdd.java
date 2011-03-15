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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.jms.CommonAttributes.AUTO_GROUP;
import static org.jboss.as.messaging.jms.CommonAttributes.BLOCK_ON_ACK;
import static org.jboss.as.messaging.jms.CommonAttributes.BLOCK_ON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.CommonAttributes.BLOCK_ON_NON_DURABLE_SEND;
import static org.jboss.as.messaging.jms.CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT;
import static org.jboss.as.messaging.jms.CommonAttributes.CALL_TIMEOUT;
import static org.jboss.as.messaging.jms.CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD;
import static org.jboss.as.messaging.jms.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.jms.CommonAttributes.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTION_TTL;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTOR_BACKUP_NAME;
import static org.jboss.as.messaging.jms.CommonAttributes.CONSUMER_MAX_RATE;
import static org.jboss.as.messaging.jms.CommonAttributes.CONSUMER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.jms.CommonAttributes.DISCOVERY_INITIAL_WAIT_TIMEOUT;
import static org.jboss.as.messaging.jms.CommonAttributes.DUPS_OK_BATCH_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.jms.CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION;
import static org.jboss.as.messaging.jms.CommonAttributes.FAILOVER_ON_SERVER_SHUTDOWN;
import static org.jboss.as.messaging.jms.CommonAttributes.GROUP_ID;
import static org.jboss.as.messaging.jms.CommonAttributes.LOAD_BALANCING_CLASS_NAME;
import static org.jboss.as.messaging.jms.CommonAttributes.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.jms.CommonAttributes.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.PRE_ACK;
import static org.jboss.as.messaging.jms.CommonAttributes.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.jms.CommonAttributes.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.jms.CommonAttributes.RETRY_INTERVAL;
import static org.jboss.as.messaging.jms.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.jms.CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.jms.CommonAttributes.USE_GLOBAL_POOLS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * Update adding a connection factory to the subsystem. The
 * runtime action will create the {@link ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 */
class ConnectionFactoryAdd implements ModelAddOperationHandler {

    static final ConnectionFactoryAdd INSTANCE = new ConnectionFactoryAdd();
    private static final String[] NO_BINDINGS = new String[0];

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(opAddr);

        final ModelNode subModel = context.getSubModel();
        for(final String attribute : JMSServices.CF_ATTRIBUTES) {
            if(operation.hasDefined(attribute)) {
                subModel.get(attribute).set(operation.get(attribute));
            }
        }

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ConnectionFactoryConfiguration configuration = createConfiguration(name, operation);
                    final ConnectionFactoryService service = new ConnectionFactoryService(configuration);
                    final ServiceName serviceName = JMSServices.JMS_CF_BASE.append(name);
                    context.getServiceTarget().addService(serviceName, service)
                            .addDependency(JMSServices.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                            .setInitialMode(Mode.ACTIVE)
                            .addListener(new ResultHandler.ServiceStartListener(resultHandler))
                            .install();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    static ConnectionFactoryConfiguration createConfiguration(final String name, final ModelNode operation) {
        final ConnectionFactoryConfiguration config = new ConnectionFactoryConfigurationImpl(name, jndiBindings(operation));

        config.setAutoGroup(operation.get(AUTO_GROUP).asBoolean(HornetQClient.DEFAULT_AUTO_GROUP));
        config.setBlockOnAcknowledge(operation.get(BLOCK_ON_ACK).asBoolean(HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE));
        config.setBlockOnDurableSend(operation.get(BLOCK_ON_DURABLE_SEND).asBoolean(HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND));
        config.setBlockOnNonDurableSend(operation.get(BLOCK_ON_NON_DURABLE_SEND).asBoolean(HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND));
        config.setCacheLargeMessagesClient(operation.get(CACHE_LARGE_MESSAGE_CLIENT).asBoolean(HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT));
        config.setCallTimeout(operation.get(CALL_TIMEOUT).asLong(HornetQClient.DEFAULT_CALL_TIMEOUT));
        config.setClientFailureCheckPeriod(operation.get(CLIENT_FAILURE_CHECK_PERIOD).asInt((int) HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD));
        if(operation.hasDefined(CLIENT_ID)) {
            config.setClientID(operation.get(CLIENT_ID).asString());
        }
        config.setConfirmationWindowSize(operation.get(CONFIRMATION_WINDOW_SIZE).asInt(HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE));
        config.setConnectionTTL(operation.get(CONNECTION_TTL).asLong(HornetQClient.DEFAULT_CONNECTION_TTL));
        if (operation.hasDefined(CONNECTOR)) {
            ModelNode connectorRefs = operation.get(CONNECTOR);
            List<Pair<String, String>> connectorNames = new ArrayList<Pair<String,String>>();
            for (String connectorName : operation.get(CONNECTOR).keys()) {
                ModelNode connectorRef = connectorRefs.get(connectorName);
                String backup = connectorRef.hasDefined(CONNECTOR_BACKUP_NAME) ? connectorRef.get(CONNECTOR_BACKUP_NAME).asString() : null;
                connectorNames.add( new Pair<String, String>(connectorName, backup));
            }
            config.setConnectorNames(connectorNames);
        }
        //config.setConnectorConfigs(connectorConfigs)
        // config.setConnectorNames(connectors);
        config.setConsumerMaxRate(operation.get(CONSUMER_MAX_RATE).asInt(HornetQClient.DEFAULT_CONSUMER_MAX_RATE));
        config.setConsumerWindowSize(operation.get(CONSUMER_WINDOW_SIZE).asInt(HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE));
        if(operation.hasDefined(DISCOVERY_GROUP_NAME)) {
            config.setDiscoveryGroupName(operation.get(DISCOVERY_GROUP_NAME).asString());
        }
        config.setDupsOKBatchSize(operation.get(DUPS_OK_BATCH_SIZE).asInt(HornetQClient.DEFAULT_ACK_BATCH_SIZE));
        config.setFailoverOnInitialConnection(operation.get(FAILOVER_ON_INITIAL_CONNECTION).asBoolean(HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION));
        config.setFailoverOnServerShutdown(operation.get(FAILOVER_ON_SERVER_SHUTDOWN).asBoolean(HornetQClient.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN));
        if(operation.hasDefined(GROUP_ID)) {
            config.setGroupID(operation.get(GROUP_ID).asString());
        }

        config.setInitialWaitTimeout(operation.get(DISCOVERY_INITIAL_WAIT_TIMEOUT).asLong(HornetQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT));
        if (operation.hasDefined(LOAD_BALANCING_CLASS_NAME)) {
             config.setLoadBalancingPolicyClassName(operation.get(LOAD_BALANCING_CLASS_NAME).asString());
        }
        config.setMaxRetryInterval(operation.get(MAX_RETRY_INTERVAL).asLong(HornetQClient.DEFAULT_MAX_RETRY_INTERVAL));
        config.setMinLargeMessageSize(operation.get(MIN_LARGE_MESSAGE_SIZE).asInt(HornetQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE));
        config.setPreAcknowledge(operation.get(PRE_ACK).asBoolean(HornetQClient.DEFAULT_PRE_ACKNOWLEDGE));
        config.setProducerMaxRate(operation.get(PRODUCER_MAX_RATE).asInt(HornetQClient.DEFAULT_PRODUCER_MAX_RATE));
        config.setProducerWindowSize(operation.get(PRODUCER_WINDOW_SIZE).asInt(HornetQClient.DEFAULT_PRODUCER_WINDOW_SIZE));
        config.setReconnectAttempts(operation.get(RECONNECT_ATTEMPTS).asInt(HornetQClient.DEFAULT_RECONNECT_ATTEMPTS));
        config.setRetryInterval(operation.get(RETRY_INTERVAL).asLong(HornetQClient.DEFAULT_RETRY_INTERVAL));
        config.setRetryIntervalMultiplier(operation.get(RETRY_INTERVAL_MULTIPLIER).asDouble(HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER));
        config.setScheduledThreadPoolMaxSize(operation.get(SCHEDULED_THREAD_POOL_MAX_SIZE).asInt(HornetQClient.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE));
        config.setThreadPoolMaxSize(operation.get(THREAD_POOL_MAX_SIZE).asInt(HornetQClient.DEFAULT_THREAD_POOL_MAX_SIZE));
        config.setTransactionBatchSize(operation.get(TRANSACTION_BATCH_SIZE).asInt(HornetQClient.DEFAULT_ACK_BATCH_SIZE));
        config.setUseGlobalPools(operation.get(USE_GLOBAL_POOLS).asBoolean(HornetQClient.DEFAULT_USE_GLOBAL_POOLS));

        return config;
    }

    static String[] jndiBindings(final ModelNode node) {
        if(node.hasDefined(ENTRIES)) {
            final Set<String> bindings = new HashSet<String>();
            for(final ModelNode entry : node.get(ENTRIES).asList()) {
                bindings.add(entry.asString());
            }
            return bindings.toArray(new String[bindings.size()]);
        }
        return NO_BINDINGS;
    }

    static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        for(final String attribute : JMSServices.CF_ATTRIBUTES) {
            if(subModel.hasDefined(attribute)) {
                operation.get(attribute).set(subModel.get(attribute));
            }
        }

        return operation;
    }

}
