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

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.messaging.HornetQActivationService.isHornetQServerActive;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.ConnectionFactoryControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.messaging.AlternativeAttributeCheckHandler;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Write attribute handler for attributes that update a JMS connection factory configuration.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ConnectionFactoryWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public static final ConnectionFactoryWriteAttributeHandler INSTANCE = new ConnectionFactoryWriteAttributeHandler();

    private ConnectionFactoryWriteAttributeHandler() {
        super(ConnectionFactoryDefinition.ATTRIBUTES);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new AlternativeAttributeCheckHandler(ConnectionFactoryDefinition.ATTRIBUTES), MODEL);

        super.execute(context, operation);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String attributeName, final ModelNode newValue,
                                           final ModelNode currentValue,
                                           final HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        AttributeDefinition attr = getAttributeDefinition(attributeName);
        if (attr.getFlags().contains(AttributeAccess.Flag.RESTART_ALL_SERVICES)) {
            // Restart required
            return true;
        }

        ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = registry.getService(hqServiceName);
        if (hqService == null) {
            // The service isn't installed, so the work done in the Stage.MODEL part is all there is to it
            return false;
        } else if (hqService.getState() != ServiceController.State.UP) {
            // Service is installed but not up?
            //throw new IllegalStateException(String.format("Cannot apply attribute %s to runtime; service %s is not in state %s, it is in state %s",
            //            attributeName, MessagingServices.JBOSS_MESSAGING, ServiceController.State.UP, hqService.getState()));
            // No, don't barf; just let the update apply to the model and put the server in a reload-required state
            return true;
        } else {
            if (!isHornetQServerActive(context, operation)) {
                return false;
            }
            // Actually apply the update
            applyOperationToHornetQService(context, getName(operation), attributeName, newValue, hqService);
            return false;
        }
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                         final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert,
                                         final Void handback) throws OperationFailedException {
            ServiceRegistry registry = context.getServiceRegistry(true);
            final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
            ServiceController<?> hqService = registry.getService(hqServiceName);
            if (hqService != null && hqService.getState() == ServiceController.State.UP) {
                applyOperationToHornetQService(context, getName(operation), attributeName, valueToRestore, hqService);
            }
    }

    private String getName(final ModelNode operation) {
        return PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
    }

    private void applyOperationToHornetQService(final OperationContext context, String name, String attributeName, ModelNode value, ServiceController<?> hqService) {
        if(attributeName.equals(Common.CONNECTOR.getName()) ||
                attributeName.equals(Common.DISCOVERY_GROUP_NAME.getName())) {
            return;
        }

        HornetQServer server =  HornetQServer.class.cast(hqService.getValue());
        ConnectionFactoryControl control = ConnectionFactoryControl.class.cast(server.getManagementService().getResource(ResourceNames.JMS_CONNECTION_FACTORY + name));
        try {
            if (attributeName.equals(CommonAttributes.CLIENT_ID.getName()))  {
                control.setClientID(value.isDefined() ? value.asString() : null);
            } else if (attributeName.equals(Common.COMPRESS_LARGE_MESSAGES.getName())) {
                control.setCompressLargeMessages(value.asBoolean());
            } else if (attributeName.equals(Common.CLIENT_FAILURE_CHECK_PERIOD.getName())) {
                control.setClientFailureCheckPeriod(value.asLong());
            } else if (attributeName.equals(CommonAttributes.CALL_TIMEOUT.getName())) {
                control.setCallTimeout(value.asLong());
            } else if (attributeName.equals(CommonAttributes.CALL_FAILOVER_TIMEOUT.getName())) {
                control.setCallFailoverTimeout(value.asLong());
            }else if (attributeName.equals(Common.DUPS_OK_BATCH_SIZE.getName())) {
                control.setDupsOKBatchSize(value.asInt());
            } else if (attributeName.equals(Common.CONSUMER_MAX_RATE.getName())) {
                control.setConsumerMaxRate(value.asInt());
            } else if (attributeName.equals(Common.CONSUMER_WINDOW_SIZE.getName())) {
                control.setConsumerWindowSize(value.asInt());
            } else if (attributeName.equals(Common.PRODUCER_MAX_RATE.getName())) {
                control.setProducerMaxRate(value.asInt());
            } else if (attributeName.equals(Common.CONFIRMATION_WINDOW_SIZE.getName())) {
                control.setConfirmationWindowSize(value.asInt());
            } else if (attributeName.equals(Common.BLOCK_ON_ACKNOWLEDGE.getName())) {
                control.setBlockOnAcknowledge(value.asBoolean());
            } else if (attributeName.equals(Common.BLOCK_ON_DURABLE_SEND.getName())) {
                control.setBlockOnDurableSend(value.asBoolean());
            } else if (attributeName.equals(Common.BLOCK_ON_NON_DURABLE_SEND.getName())) {
                control.setBlockOnNonDurableSend(value.asBoolean());
            } else if (attributeName.equals(Common.PRE_ACKNOWLEDGE.getName())) {
                control.setPreAcknowledge(value.asBoolean());
            } else if (attributeName.equals(Common.CONNECTION_TTL.getName())) {
                control.setConnectionTTL(value.asLong());
            } else if (attributeName.equals(Common.TRANSACTION_BATCH_SIZE.getName())) {
                control.setTransactionBatchSize(value.asInt());
            } else if (attributeName.equals(CommonAttributes.MIN_LARGE_MESSAGE_SIZE.getName())) {
                control.setMinLargeMessageSize(value.asInt());
            } else if (attributeName.equals(Common.AUTO_GROUP.getName())) {
                control.setAutoGroup(value.asBoolean());
            } else if (attributeName.equals(Common.RETRY_INTERVAL.getName())) {
                control.setRetryInterval(value.asLong());
            } else if (attributeName.equals(Common.RETRY_INTERVAL_MULTIPLIER.getName())) {
                control.setRetryIntervalMultiplier(value.asDouble());
            } else if (attributeName.equals(Common.RECONNECT_ATTEMPTS.getName())) {
                control.setReconnectAttempts(value.asInt());
            } else if (attributeName.equals(Common.FAILOVER_ON_INITIAL_CONNECTION.getName())) {
                control.setFailoverOnInitialConnection(value.asBoolean());
            } else if (attributeName.equals(Common.PRODUCER_WINDOW_SIZE.getName())) {
                control.setProducerWindowSize(value.asInt());
            } else if (attributeName.equals(Common.CACHE_LARGE_MESSAGE_CLIENT.getName())) {
                control.setCacheLargeMessagesClient(value.asBoolean());
            } else if (attributeName.equals(CommonAttributes.MAX_RETRY_INTERVAL.getName())) {
                control.setMaxRetryInterval(value.asLong());
            } else if (attributeName.equals(Common.SCHEDULED_THREAD_POOL_MAX_SIZE.getName())) {
                control.setScheduledThreadPoolMaxSize(value.asInt());
            } else if (attributeName.equals(Common.THREAD_POOL_MAX_SIZE.getName())) {
                control.setThreadPoolMaxSize(value.asInt());
            } else if (attributeName.equals(Common.GROUP_ID.getName())) {
                control.setGroupID(value.isDefined() ? value.asString() : null);
            } else if (attributeName.equals(Common.USE_GLOBAL_POOLS.getName())) {
                control.setUseGlobalPools(value.asBoolean());
            } else if (attributeName.equals(Common.CONNECTION_LOAD_BALANCING_CLASS_NAME.getName())) {
                control.setConnectionLoadBalancingPolicyClassName(value.asString());
            } else {
                // Bug! Someone added the attribute to the set but did not implement
                throw MessagingLogger.ROOT_LOGGER.unsupportedRuntimeAttribute(attributeName);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
