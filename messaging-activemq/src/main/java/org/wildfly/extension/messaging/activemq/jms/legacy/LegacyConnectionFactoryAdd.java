/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms.legacy;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CLIENT_ID;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.AUTO_GROUP;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.BLOCK_ON_ACKNOWLEDGE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.BLOCK_ON_DURABLE_SEND;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.BLOCK_ON_NON_DURABLE_SEND;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CACHE_LARGE_MESSAGE_CLIENT;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CLIENT_FAILURE_CHECK_PERIOD;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.COMPRESS_LARGE_MESSAGES;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONFIRMATION_WINDOW_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONNECTION_LOAD_BALANCING_CLASS_NAME;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONNECTION_TTL;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONNECTORS;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONSUMER_MAX_RATE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONSUMER_WINDOW_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.DISCOVERY_GROUP;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.DUPS_OK_BATCH_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.FAILOVER_ON_INITIAL_CONNECTION;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.GROUP_ID;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.INITIAL_CONNECT_ATTEMPTS;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.INITIAL_MESSAGE_PACKET_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.MAX_RETRY_INTERVAL;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.MIN_LARGE_MESSAGE_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.PRE_ACKNOWLEDGE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.PRODUCER_MAX_RATE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.PRODUCER_WINDOW_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.RECONNECT_ATTEMPTS;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.RETRY_INTERVAL;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.RETRY_INTERVAL_MULTIPLIER;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.TRANSACTION_BATCH_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.USE_GLOBAL_POOLS;

import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryAdd extends AbstractAddStepHandler {

    static final LegacyConnectionFactoryAdd INSTANCE = new LegacyConnectionFactoryAdd();

    public LegacyConnectionFactoryAdd() {
        super(LegacyConnectionFactoryDefinition.ATTRIBUTES);
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        final ServiceName activeMQServerServiceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());

        HornetQConnectionFactory incompleteCF = createLegacyConnectionFactory(context, model);
        ModelNode discoveryGroup = DISCOVERY_GROUP.resolveModelAttribute(context, model);
        String discoveryGroupName = discoveryGroup.isDefined() ? discoveryGroup.asString() : null;

        LegacyConnectionFactoryService service = LegacyConnectionFactoryService.installService(name, activeMQServerServiceName, context.getServiceTarget(), incompleteCF, discoveryGroupName, CONNECTORS.unwrap(context, model));
        for (String legacyEntry : LegacyConnectionFactoryDefinition.ENTRIES.unwrap(context, model)) {
            BinderServiceUtil.installBinderService(context.getServiceTarget(), legacyEntry, service, new ServiceName[0]);
        }
    }

    private HornetQConnectionFactory createLegacyConnectionFactory(OperationContext context, ModelNode model) throws OperationFailedException {

        boolean ha = LegacyConnectionFactoryDefinition.HA.resolveModelAttribute(context, model).asBoolean();
        String factoryTypeStr = LegacyConnectionFactoryDefinition.FACTORY_TYPE.resolveModelAttribute(context, model).asString();
        JMSFactoryType factoryType = LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.valueOf(factoryTypeStr).getType();

        final HornetQConnectionFactory incompleteCF;
        if (ha) {
            incompleteCF = HornetQJMSClient.createConnectionFactoryWithHA(factoryType);
        } else {
            incompleteCF = HornetQJMSClient.createConnectionFactoryWithoutHA(factoryType);
        }

        incompleteCF.setAutoGroup(AUTO_GROUP.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setBlockOnAcknowledge(BLOCK_ON_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setBlockOnDurableSend(BLOCK_ON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setBlockOnNonDurableSend(BLOCK_ON_NON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setCacheLargeMessagesClient(CACHE_LARGE_MESSAGE_CLIENT.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setCallFailoverTimeout(CALL_FAILOVER_TIMEOUT.resolveModelAttribute(context, model).asLong());
        incompleteCF.setCallTimeout(CALL_TIMEOUT.resolveModelAttribute(context, model).asLong());
        incompleteCF.setClientFailureCheckPeriod(CLIENT_FAILURE_CHECK_PERIOD.resolveModelAttribute(context, model).asLong());
        final ModelNode clientID = CLIENT_ID.resolveModelAttribute(context, model);
        if (clientID.isDefined()) {
            incompleteCF.setClientID(clientID.asString());
        }
        incompleteCF.setCompressLargeMessage(COMPRESS_LARGE_MESSAGES.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setConfirmationWindowSize(CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());

        final ModelNode connectionLoadBalancingClassName = CONNECTION_LOAD_BALANCING_CLASS_NAME.resolveModelAttribute(context, model);
        if (connectionLoadBalancingClassName.isDefined()) {
            incompleteCF.setConnectionLoadBalancingPolicyClassName(connectionLoadBalancingClassName.asString());
        }

        incompleteCF.setConnectionTTL(CONNECTION_TTL.resolveModelAttribute(context, model).asLong());
        incompleteCF.setConsumerMaxRate(CONSUMER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setConsumerWindowSize(CONSUMER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setConfirmationWindowSize(CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setDupsOKBatchSize(DUPS_OK_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setFailoverOnInitialConnection(FAILOVER_ON_INITIAL_CONNECTION.resolveModelAttribute(context, model).asBoolean());
        final ModelNode groupID = GROUP_ID.resolveModelAttribute(context, model);
        if (groupID.isDefined()) {
            incompleteCF.setGroupID(groupID.asString());
        }
        incompleteCF.setInitialConnectAttempts(INITIAL_CONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt());
        incompleteCF.setInitialMessagePacketSize(INITIAL_MESSAGE_PACKET_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setMaxRetryInterval(MAX_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        incompleteCF.setMinLargeMessageSize(MIN_LARGE_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setPreAcknowledge(PRE_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setProducerMaxRate(PRODUCER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setProducerWindowSize(PRODUCER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setReconnectAttempts(RECONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt());
        incompleteCF.setRetryInterval(RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        incompleteCF.setRetryIntervalMultiplier(RETRY_INTERVAL_MULTIPLIER.resolveModelAttribute(context, model).asDouble());
        incompleteCF.setScheduledThreadPoolMaxSize(SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setThreadPoolMaxSize(THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setTransactionBatchSize(TRANSACTION_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setUseGlobalPools(USE_GLOBAL_POOLS.resolveModelAttribute(context, model).asBoolean());
        return incompleteCF;
    }
}