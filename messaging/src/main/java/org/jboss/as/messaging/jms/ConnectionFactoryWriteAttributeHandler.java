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

package org.jboss.as.messaging.jms;

import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.ConnectionFactoryControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Write attribute handler for attributes that update a JMS connection factory configuration.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ConnectionFactoryWriteAttributeHandler extends ServerWriteAttributeOperationHandler {

    public static final ConnectionFactoryWriteAttributeHandler INSTANCE = new ConnectionFactoryWriteAttributeHandler();

    private final Map<String, AttributeDefinition> attributes = new HashMap<String, AttributeDefinition>();
    private final Map<String, AttributeDefinition> runtimeAttributes = new HashMap<String, AttributeDefinition>();
    private ConnectionFactoryWriteAttributeHandler() {
        for (AttributeDefinition attr : JMSServices.CONNECTION_FACTORY_ATTRS) {
            attributes.put(attr.getName(), attr);
        }
        for (AttributeDefinition attr : JMSServices.CONNECTION_FACTORY_WRITE_ATTRS) {
            runtimeAttributes.put(attr.getName(), attr);
        }
    }

    @Override
    protected void validateValue(String name, ModelNode value) throws OperationFailedException {
        AttributeDefinition attr = attributes.get(name);
        attr.getValidator().validateParameter(name, value);
    }

    @Override
    protected void validateResolvedValue(String name, ModelNode value) throws OperationFailedException {
        AttributeDefinition attr = attributes.get(name);
        attr.getValidator().validateResolvedParameter(name, value);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
        AttributeDefinition attr = runtimeAttributes.get(attributeName);
        if (attr != null) {
            ServiceRegistry registry = context.getServiceRegistry(true);
            ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
            if (hqService != null && hqService.getState() == ServiceController.State.UP) {
                final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
                HornetQServer server =  HornetQServer.class.cast(hqService.getValue());
                ConnectionFactoryControl control = ConnectionFactoryControl.class.cast(server.getManagementService().getResource(ResourceNames.JMS_CONNECTION_FACTORY + name));
                try {
                    if (attributeName.equals(CommonAttributes.CLIENT_ID.getName()))  {
                        final ModelNode node = CommonAttributes.CLIENT_ID.validateResolvedOperation(operation);
                        control.setClientID(node.isDefined() ? node.asString() : null);
                    } else if (attributeName.equals(CommonAttributes.COMPRESS_LARGE_MESSAGES.getName())) {
                        control.setCompressLargeMessages(CommonAttributes.COMPRESS_LARGE_MESSAGES.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD.getName())) {
                        control.setClientFailureCheckPeriod(CommonAttributes.CLIENT_FAILURE_CHECK_PERIOD.validateResolvedOperation(operation).asLong());
                    } else if (attributeName.equals(CommonAttributes.CALL_TIMEOUT.getName())) {
                        control.setCallTimeout(CommonAttributes.CALL_TIMEOUT.validateResolvedOperation(operation).asLong());
                    } else if (attributeName.equals(CommonAttributes.DUPS_OK_BATCH_SIZE.getName())) {
                        control.setDupsOKBatchSize(CommonAttributes.DUPS_OK_BATCH_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.CONSUMER_MAX_RATE.getName())) {
                        control.setConsumerMaxRate(CommonAttributes.CONSUMER_MAX_RATE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.CONSUMER_WINDOW_SIZE.getName())) {
                        control.setConsumerWindowSize(CommonAttributes.CONSUMER_WINDOW_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.PRODUCER_MAX_RATE.getName())) {
                        control.setProducerMaxRate(CommonAttributes.PRODUCER_MAX_RATE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.CONFIRMATION_WINDOW_SIZE.getName())) {
                        control.setConfirmationWindowSize(CommonAttributes.CONFIRMATION_WINDOW_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.BLOCK_ON_ACK.getName())) {
                        control.setBlockOnAcknowledge(CommonAttributes.BLOCK_ON_ACK.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.BLOCK_ON_DURABLE_SEND.getName())) {
                        control.setBlockOnDurableSend(CommonAttributes.BLOCK_ON_DURABLE_SEND.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.BLOCK_ON_NON_DURABLE_SEND.getName())) {
                        control.setBlockOnNonDurableSend(CommonAttributes.BLOCK_ON_NON_DURABLE_SEND.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.PRE_ACK.getName())) {
                        control.setPreAcknowledge(CommonAttributes.PRE_ACK.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.CONNECTION_TTL.getName())) {
                        control.setConnectionTTL(CommonAttributes.CONNECTION_TTL.validateResolvedOperation(operation).asLong());
                    } else if (attributeName.equals(CommonAttributes.TRANSACTION_BATCH_SIZE.getName())) {
                        control.setTransactionBatchSize(CommonAttributes.TRANSACTION_BATCH_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.MIN_LARGE_MESSAGE_SIZE.getName())) {
                        control.setMinLargeMessageSize(CommonAttributes.MIN_LARGE_MESSAGE_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.AUTO_GROUP.getName())) {
                        control.setAutoGroup(CommonAttributes.AUTO_GROUP.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.RETRY_INTERVAL.getName())) {
                        control.setRetryInterval(CommonAttributes.RETRY_INTERVAL.validateResolvedOperation(operation).asLong());
                    } else if (attributeName.equals(CommonAttributes.RETRY_INTERVAL_MULTIPLIER.getName())) {
                        control.setRetryIntervalMultiplier(CommonAttributes.RETRY_INTERVAL_MULTIPLIER.validateResolvedOperation(operation).asDouble());
                    } else if (attributeName.equals(CommonAttributes.CONNECTION_FACTORY_RECONNECT_ATTEMPTS.getName())) {
                        control.setReconnectAttempts(CommonAttributes.CONNECTION_FACTORY_RECONNECT_ATTEMPTS.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION.getName())) {
                        control.setFailoverOnInitialConnection(CommonAttributes.FAILOVER_ON_INITIAL_CONNECTION.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.PRODUCER_WINDOW_SIZE.getName())) {
                        control.setProducerWindowSize(CommonAttributes.PRODUCER_WINDOW_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT.getName())) {
                        control.setCacheLargeMessagesClient(CommonAttributes.CACHE_LARGE_MESSAGE_CLIENT.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.MAX_RETRY_INTERVAL.getName())) {
                        control.setMaxRetryInterval(CommonAttributes.MAX_RETRY_INTERVAL.validateResolvedOperation(operation).asLong());
                    } else if (attributeName.equals(CommonAttributes.CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE.getName())) {
                        control.setScheduledThreadPoolMaxSize(CommonAttributes.CONNECTION_SCHEDULED_THREAD_POOL_MAX_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.CONNECTION_THREAD_POOL_MAX_SIZE.getName())) {
                        control.setThreadPoolMaxSize(CommonAttributes.CONNECTION_THREAD_POOL_MAX_SIZE.validateResolvedOperation(operation).asInt());
                    } else if (attributeName.equals(CommonAttributes.GROUP_ID.getName())) {
                        final ModelNode node = CommonAttributes.GROUP_ID.validateResolvedOperation(operation);
                        control.setGroupID(node.isDefined() ? node.asString() : null);
                    } else if (attributeName.equals(CommonAttributes.USE_GLOBAL_POOLS.getName())) {
                        control.setUseGlobalPools(CommonAttributes.USE_GLOBAL_POOLS.validateResolvedOperation(operation).asBoolean());
                    } else if (attributeName.equals(CommonAttributes.LOAD_BALANCING_CLASS_NAME.getName())) {
                        control.setConnectionLoadBalancingPolicyClassName(CommonAttributes.LOAD_BALANCING_CLASS_NAME.validateResolvedOperation(operation).asString());
                    } else {
                        // Bug! Someone added the attribute to the set but did not implement
                        throw new UnsupportedOperationException(String.format("Runtime handling for %s is not implemented", attributeName));
                    }

                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return false;

            } else {
                throw new IllegalStateException(String.format("Cannot apply attribue %s ti runtime; service %s is not in state %s, it is in state %s",
                            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
            }

        } else {
            // Not a runtime attribute; restart required
            return true;
        }
    }

}
