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

package org.wildfly.extension.messaging.activemq.jms;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CLIENT_ID;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA;

import java.util.List;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.apache.activemq.artemis.jms.server.config.ConnectionFactoryConfiguration;
import org.apache.activemq.artemis.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.ActiveMQActivationService;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;

/**
 * Update adding a connection factory to the subsystem. The
 * runtime action will create the {@link ConnectionFactoryService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class ConnectionFactoryAdd extends AbstractAddStepHandler {

    public static final ConnectionFactoryAdd INSTANCE = new ConnectionFactoryAdd();

    private ConnectionFactoryAdd() {
        super(ConnectionFactoryDefinition.ATTRIBUTES);

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceName activeMQServiceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        final ConnectionFactoryConfiguration configuration = createConfiguration(context, name, model);
        final ConnectionFactoryService service = new ConnectionFactoryService(configuration);
        final ServiceName serviceName = JMSServices.getConnectionFactoryBaseServiceName(activeMQServiceName).append(name);
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(serviceName, service)
                .addDependency(ActiveMQActivationService.getServiceName(activeMQServiceName))
                .addDependency(JMSServices.getJmsManagerBaseServiceName(activeMQServiceName), JMSServerManager.class, service.getJmsServer())
                .setInitialMode(Mode.PASSIVE);
        org.jboss.as.server.Services.addServerExecutorDependency(serviceBuilder, service.getExecutorInjector());
        serviceBuilder.install();
    }

    static ConnectionFactoryConfiguration createConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final List<String> entries = Common.ENTRIES.unwrap(context, model);

        final ConnectionFactoryConfiguration config = new ConnectionFactoryConfigurationImpl()
                .setName(name)
                .setHA(ActiveMQClient.DEFAULT_HA)
                .setBindings(entries.toArray(new String[entries.size()]));

        config.setHA(HA.resolveModelAttribute(context, model).asBoolean());
        config.setAutoGroup(Common.AUTO_GROUP.resolveModelAttribute(context, model).asBoolean());
        config.setBlockOnAcknowledge(Common.BLOCK_ON_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        config.setBlockOnDurableSend(Common.BLOCK_ON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        config.setBlockOnNonDurableSend(Common.BLOCK_ON_NON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        config.setCacheLargeMessagesClient(Common.CACHE_LARGE_MESSAGE_CLIENT.resolveModelAttribute(context, model).asBoolean());
        config.setCallTimeout(CALL_TIMEOUT.resolveModelAttribute(context, model).asLong());
        config.setClientFailureCheckPeriod(Common.CLIENT_FAILURE_CHECK_PERIOD.resolveModelAttribute(context, model).asInt());
        config.setCallFailoverTimeout(CALL_FAILOVER_TIMEOUT.resolveModelAttribute(context, model).asLong());
        final ModelNode clientId = CLIENT_ID.resolveModelAttribute(context, model);
        if (clientId.isDefined()) {
            config.setClientID(clientId.asString());
        }
        config.setCompressLargeMessages(Common.COMPRESS_LARGE_MESSAGES.resolveModelAttribute(context, model).asBoolean());
        config.setConfirmationWindowSize(Common.CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        config.setConnectionTTL(Common.CONNECTION_TTL.resolveModelAttribute(context, model).asLong());
        List<String> connectorNames = Common.CONNECTORS.unwrap(context, model);
        config.setConnectorNames(connectorNames);
        config.setConsumerMaxRate(Common.CONSUMER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        config.setConsumerWindowSize(Common.CONSUMER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        final ModelNode discoveryGroupName = Common.DISCOVERY_GROUP.resolveModelAttribute(context, model);
        if (discoveryGroupName.isDefined()) {
            config.setDiscoveryGroupName(discoveryGroupName.asString());
        }

        config.setDupsOKBatchSize(Common.DUPS_OK_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        config.setFailoverOnInitialConnection(Common.FAILOVER_ON_INITIAL_CONNECTION.resolveModelAttribute(context, model).asBoolean());

        final ModelNode groupId = Common.GROUP_ID.resolveModelAttribute(context, model);
        if (groupId.isDefined()) {
            config.setGroupID(groupId.asString());
        }

        final ModelNode lbcn = Common.CONNECTION_LOAD_BALANCING_CLASS_NAME.resolveModelAttribute(context, model);
        if (lbcn.isDefined()) {
            config.setLoadBalancingPolicyClassName(lbcn.asString());
        }
        config.setMaxRetryInterval(Common.MAX_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        config.setMinLargeMessageSize(Common.MIN_LARGE_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt());
        config.setPreAcknowledge(Common.PRE_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        config.setProducerMaxRate(Common.PRODUCER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        config.setProducerWindowSize(Common.PRODUCER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        config.setReconnectAttempts(Common.RECONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt());
        config.setRetryInterval(Common.RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        config.setRetryIntervalMultiplier(Common.RETRY_INTERVAL_MULTIPLIER.resolveModelAttribute(context, model).asDouble());
        config.setScheduledThreadPoolMaxSize(Common.SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        config.setThreadPoolMaxSize(Common.THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        config.setTransactionBatchSize(Common.TRANSACTION_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        config.setUseGlobalPools(Common.USE_GLOBAL_POOLS.resolveModelAttribute(context, model).asBoolean());
        config.setLoadBalancingPolicyClassName(Common.CONNECTION_LOAD_BALANCING_CLASS_NAME.resolveModelAttribute(context, model).asString());
        final ModelNode clientProtocolManagerFactory = Common.PROTOCOL_MANAGER_FACTORY.resolveModelAttribute(context, model);
        if (clientProtocolManagerFactory.isDefined()) {
            config.setProtocolManagerFactoryStr(clientProtocolManagerFactory.asString());
        }
        List<String> deserializationBlackList = Common.DESERIALIZATION_BLACKLIST.unwrap(context, model);
        if (deserializationBlackList.size() > 0) {
            config.setDeserializationBlackList(String.join(",", deserializationBlackList));
        }
        List<String> deserializationWhiteList = Common.DESERIALIZATION_WHITELIST.unwrap(context, model);
        if (deserializationWhiteList.size() > 0) {
            config.setDeserializationWhiteList(String.join(",", deserializationWhiteList));
        }
        JMSFactoryType jmsFactoryType = ConnectionFactoryType.valueOf(ConnectionFactoryAttributes.Regular.FACTORY_TYPE.resolveModelAttribute(context, model).asString()).getType();
        config.setFactoryType(jmsFactoryType);

        config.setInitialMessagePacketSize(ConnectionFactoryAttributes.Common.INITIAL_MESSAGE_PACKET_SIZE.resolveModelAttribute(context, model).asInt());
        return config;
    }
}
