/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.JChannel;

/**
 * Handler for reading run-time only attributes from an underlying channel service.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ChannelMetricsHandler extends AbstractRuntimeOnlyHandler {

    public static final ChannelMetricsHandler INSTANCE = new ChannelMetricsHandler();

    public enum ChannelMetrics {
        ADDRESS(ChannelInstanceResourceDefinition.ADDRESS),
        ADDRESS_AS_UUID(ChannelInstanceResourceDefinition.ADDRESS_AS_UUID),
        DISCARD_OWN_MESSAGES(ChannelInstanceResourceDefinition.DISCARD_OWN_MESSAGES),
        NUM_TASKS_IN_TIMER(ChannelInstanceResourceDefinition.NUM_TASKS_IN_TIMER),
        NUM_TIMER_THREADS(ChannelInstanceResourceDefinition.NUM_TIMER_THREADS),
        RECEIVED_BYTES(ChannelInstanceResourceDefinition.RECEIVED_BYTES),
        RECEIVED_MESSAGES(ChannelInstanceResourceDefinition.RECEIVED_MESSAGES),
        SENT_BYTES(ChannelInstanceResourceDefinition.SENT_BYTES),
        SENT_MESSAGES(ChannelInstanceResourceDefinition.SENT_MESSAGES),
        STATE(ChannelInstanceResourceDefinition.STATE),
        STATS_ENABLED(ChannelInstanceResourceDefinition.STATS_ENABLED),
        VERSION(ChannelInstanceResourceDefinition.VERSION),
        VIEW(ChannelInstanceResourceDefinition.VIEW);

        private static final Map<String, ChannelMetrics> MAP = new HashMap<String, ChannelMetrics>();

        static {
            for (ChannelMetrics metric : ChannelMetrics.values()) {
                MAP.put(metric.toString(), metric);
            }
        }

        final AttributeDefinition definition ;

        private ChannelMetrics(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public String toString() {
            return definition.getName();
        }

        public static ChannelMetrics getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        // get the channel name and channel attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size()-1).getValue();
        String attrName = operation.require(NAME).asString();
        ChannelMetrics metric = ChannelMetrics.getStat(attrName);

        // lookup the channel
        ServiceName channelServiceName = ChannelInstanceResource.CHANNEL_PARENT.append(channelName);
        ServiceController<?> controller = context.getServiceRegistry(false).getService(channelServiceName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getValue() != null;
        ModelNode result = new ModelNode();

        if (metric == null) {
            context.getFailureDescription().set(JGroupsMessages.MESSAGES.unknownMetric(attrName));
        } else if (!started) {
            // when the cache service is not available, return a null result
        } else {
            JChannel channel = (JChannel) controller.getValue();
            switch (metric) {
                case ADDRESS:
                    result.set(channel.getAddressAsString());
                    break;
                case ADDRESS_AS_UUID:
                    result.set(channel.getAddressAsUUID());
                    break;
                case DISCARD_OWN_MESSAGES:
                    result.set(channel.getDiscardOwnMessages());
                    break;
                case NUM_TASKS_IN_TIMER:
                    result.set(channel.getNumberOfTasksInTimer());
                    break;
                case NUM_TIMER_THREADS:
                    result.set(channel.getTimerThreads());
                    break;
                case RECEIVED_BYTES:
                    result.set(channel.getReceivedBytes());
                    break;
                case RECEIVED_MESSAGES:
                    result.set(channel.getReceivedMessages());
                    break;
                case SENT_BYTES:
                    result.set(channel.getSentBytes());
                    break;
                case SENT_MESSAGES:
                    result.set(channel.getSentMessages());
                    break;
                case STATE:
                    result.set(channel.getState());
                    break;
                case STATS_ENABLED:
                    result.set(channel.statsEnabled());
                    break;
                case VERSION:
                    result.set(JChannel.getVersion());
                    break;
                case VIEW:
                    result.set(channel.getViewAsString());
                    break;
            }
            context.getResult().set(result);
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }
}
