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

    ADDRESS("address", ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getAddressAsString());
        }
    },
    ADDRESS_AS_UUID("address-as-uuid", ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getAddressAsUUID());
        }
    },
    DISCARD_OWN_MESSAGES("discard-own-messages", ModelType.BOOLEAN) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getDiscardOwnMessages());
        }
    },
    NUM_TASKS_IN_TIMER("num-tasks-in-timer", ModelType.INT, JGroupsModel.VERSION_5_0_0) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(0);
        }
    },
    NUM_TIMER_THREADS("num-timer-threads", ModelType.INT, JGroupsModel.VERSION_5_0_0) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(0);
        }
    },
    RECEIVED_BYTES("received-bytes", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getReceivedBytes());
        }
    },
    RECEIVED_MESSAGES("received-messages", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getReceivedMessages());
        }
    },
    SENT_BYTES("sent-bytes", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getSentBytes());
        }
    },
    SENT_MESSAGES("sent-messages", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getSentMessages());
        }
    },
    STATE("state", ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getState());
        }
    },
    VERSION("version", ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(JChannel.getVersion());
        }
    },
    VIEW("view", ModelType.STRING) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getViewAsString());
        }
    },
    ;
    private final AttributeDefinition definition;

    ChannelMetric(String name, ModelType type) {
        this(name, type, null);
    }

    ChannelMetric(String name, ModelType type, JGroupsModel deprecation) {
        SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, type, true).setStorageRuntime();
        if (deprecation != null) {
            builder.setDeprecated(deprecation.getVersion());
        }
        this.definition = builder.build();
    }

    @Override
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}