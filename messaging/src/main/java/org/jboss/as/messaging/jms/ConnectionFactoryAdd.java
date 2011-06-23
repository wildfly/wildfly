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
import static org.jboss.as.messaging.CommonAttributes.CONFIRMATION_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_MAX_RATE;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.DUPS_OK_BATCH_SIZE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ID;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.LOAD_BALANCING_CLASS_NAME;
import static org.jboss.as.messaging.CommonAttributes.MAX_RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.MIN_LARGE_MESSAGE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.PRE_ACK;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_MAX_RATE;
import static org.jboss.as.messaging.CommonAttributes.PRODUCER_WINDOW_SIZE;
import static org.jboss.as.messaging.CommonAttributes.RECONNECT_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.messaging.CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_BATCH_SIZE;
import static org.jboss.as.messaging.CommonAttributes.USE_GLOBAL_POOLS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.messaging.jms.JMSServices.NodeAttribute;
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
    private static final String[] NO_BINDINGS = new String[0];

    protected void populateModel(ModelNode operation, ModelNode model) {
        for (final NodeAttribute attribute : JMSServices.CONNECTION_FACTORY_ATTRS) {
            final String attrName = attribute.getName();
            if (operation.hasDefined(attrName)) {
                model.get(attrName).set(operation.get(attrName));
            }
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ConnectionFactoryConfiguration configuration = createConfiguration(name, operation);
        final ConnectionFactoryService service = new ConnectionFactoryService(configuration);
        final ServiceName serviceName = JMSServices.JMS_CF_BASE.append(name);
        newControllers.add(context.getServiceTarget().addService(serviceName, service)
                .addDependency(JMSServices.JMS_MANAGER, JMSServerManager.class, service.getJmsServer())
                .addListener(verificationHandler)
                .setInitialMode(Mode.ACTIVE)
                .install());
    }

    static ConnectionFactoryConfiguration createConfiguration(final String name, final ModelNode operation) {
        final ConnectionFactoryConfiguration config = new ConnectionFactoryConfigurationImpl(name, HornetQClient.DEFAULT_HA, jndiBindings(operation));

        config.setHA(operation.get(HA).asBoolean(HornetQClient.DEFAULT_HA));
        config.setAutoGroup(operation.get(AUTO_GROUP).asBoolean(HornetQClient.DEFAULT_AUTO_GROUP));
        config.setBlockOnAcknowledge(operation.get(BLOCK_ON_ACK).asBoolean(HornetQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE));
        config.setBlockOnDurableSend(operation.get(BLOCK_ON_DURABLE_SEND).asBoolean(HornetQClient.DEFAULT_BLOCK_ON_DURABLE_SEND));
        config.setBlockOnNonDurableSend(operation.get(BLOCK_ON_NON_DURABLE_SEND).asBoolean(HornetQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND));
        config.setCacheLargeMessagesClient(operation.get(CACHE_LARGE_MESSAGE_CLIENT).asBoolean(HornetQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT));
        config.setCallTimeout(operation.get(CALL_TIMEOUT).asLong(HornetQClient.DEFAULT_CALL_TIMEOUT));
        config.setClientFailureCheckPeriod(operation.get(CLIENT_FAILURE_CHECK_PERIOD).asInt((int) HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD));
        if (operation.hasDefined(CLIENT_ID)) {
            config.setClientID(operation.get(CLIENT_ID).asString());
        }
        config.setConfirmationWindowSize(operation.get(CONFIRMATION_WINDOW_SIZE).asInt(HornetQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE));
        config.setConnectionTTL(operation.get(CONNECTION_TTL).asLong(HornetQClient.DEFAULT_CONNECTION_TTL));
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
        config.setConsumerMaxRate(operation.get(CONSUMER_MAX_RATE).asInt(HornetQClient.DEFAULT_CONSUMER_MAX_RATE));
        config.setConsumerWindowSize(operation.get(CONSUMER_WINDOW_SIZE).asInt(HornetQClient.DEFAULT_CONSUMER_WINDOW_SIZE));
        if (operation.hasDefined(DISCOVERY_GROUP_NAME)) {
            config.setDiscoveryGroupName(operation.get(DISCOVERY_GROUP_NAME).asString());
        }
        config.setDupsOKBatchSize(operation.get(DUPS_OK_BATCH_SIZE).asInt(HornetQClient.DEFAULT_ACK_BATCH_SIZE));
        config.setFailoverOnInitialConnection(operation.get(FAILOVER_ON_INITIAL_CONNECTION).asBoolean(HornetQClient.DEFAULT_FAILOVER_ON_INITIAL_CONNECTION));
        if(operation.hasDefined(GROUP_ID)) {
            config.setGroupID(operation.get(GROUP_ID).asString());
        }

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
        if (node.hasDefined(ENTRIES)) {
            final Set<String> bindings = new HashSet<String>();
            for (final ModelNode entry : node.get(ENTRIES).asList()) {
                bindings.add(entry.asString());
            }
            return bindings.toArray(new String[bindings.size()]);
        }
        return NO_BINDINGS;
    }

    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        for (final NodeAttribute attribute : JMSServices.CONNECTION_FACTORY_ATTRS) {
            final String attrName = attribute.getName();
            if (subModel.has(attrName)) {
                operation.get(attrName).set(subModel.get(attrName));
            }
        }

        return operation;
    }

}
