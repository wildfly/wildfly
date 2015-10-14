/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.JChannel;

/**
 * Enumerates management metrics for a channel.
 * @author Paul Ferraro
 */
public enum ChannelMetric implements Metric<JChannel> {

    ADDRESS(MetricKeys.ADDRESS, ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getAddressAsString());
        }
    },
    ADDRESS_AS_UUID(MetricKeys.ADDRESS_AS_UUID, ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getAddressAsUUID());
        }
    },
    DISCARD_OWN_MESSAGES(MetricKeys.DISCARD_OWN_MESSAGES, ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getDiscardOwnMessages());
        }
    },
    NUM_TASKS_IN_TIMER(MetricKeys.NUM_TASKS_IN_TIMER, ModelType.INT) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getNumberOfTasksInTimer());
        }
    },
    NUM_TIMER_THREADS(MetricKeys.NUM_TIMER_THREADS, ModelType.INT) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getTimerThreads());
        }
    },
    RECEIVED_BYTES(MetricKeys.RECEIVED_BYTES, ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getReceivedBytes());
        }
    },
    RECEIVED_MESSAGES(MetricKeys.RECEIVED_MESSAGES, ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getReceivedMessages());
        }
    },
    SENT_BYTES(MetricKeys.SENT_BYTES, ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getSentBytes());
        }
    },
    SENT_MESSAGES(MetricKeys.SENT_MESSAGES, ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getSentMessages());
        }
    },
    STATE(MetricKeys.STATE, ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getState());
        }
    },
    STATS_ENABLED(MetricKeys.STATS_ENABLED, ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.statsEnabled());
        }
    },
    VERSION(MetricKeys.VERSION, ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(JChannel.getVersion());
        }
    },
    VIEW(MetricKeys.VIEW, ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getViewAsString());
        }
    },
    ;
    private final AttributeDefinition definition;

    private ChannelMetric(String name, ModelType type) {
        this.definition = new SimpleAttributeDefinitionBuilder(name, type, true).setStorageRuntime().build();
    }

    @Override
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}