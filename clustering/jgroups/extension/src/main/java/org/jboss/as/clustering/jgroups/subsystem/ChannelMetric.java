/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.JChannel;
import org.wildfly.subsystem.resource.executor.Metric;

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
    RECEIVED_BYTES("received-bytes", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getProtocolStack().getTransport().getMessageStats().getNumBytesReceived());
        }
    },
    RECEIVED_MESSAGES("received-messages", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getProtocolStack().getTransport().getMessageStats().getNumMsgsReceived());
        }
    },
    SENT_BYTES("sent-bytes", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getProtocolStack().getTransport().getMessageStats().getNumBytesSent());
        }
    },
    SENT_MESSAGES("sent-messages", ModelType.LONG) {
        @Override
        public ModelNode execute(JChannel channel) {
            return new ModelNode(channel.getProtocolStack().getTransport().getMessageStats().getNumMsgsSent());
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

    ChannelMetric(String name, ModelType type, JGroupsSubsystemModel deprecation) {
        SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, type, true).setStorageRuntime();
        if (deprecation != null) {
            builder.setDeprecated(deprecation.getVersion());
        }
        this.definition = builder.build();
    }

    @Override
    public AttributeDefinition get() {
        return this.definition;
    }
}
