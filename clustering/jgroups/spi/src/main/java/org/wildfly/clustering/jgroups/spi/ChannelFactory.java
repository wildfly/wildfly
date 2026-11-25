/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jgroups.Message;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Factory for creating JGroups channels.
 * @author Paul Ferraro
 */
public interface ChannelFactory extends org.wildfly.clustering.jgroups.ChannelFactory {

    NullaryServiceDescriptor<ChannelFactory> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.default-channel-factory", ChannelFactory.class);
    UnaryServiceDescriptor<ChannelFactory> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.channel-factory", DEFAULT_SERVICE_DESCRIPTOR);

    /**
     * Returns the protocol stack configuration of this channel factory.
     * @return the protocol stack configuration of this channel factory
     */
    ChannelFactoryConfiguration getConfiguration();

    /**
     * Determines whether or not the specified message response indicates the fork stack or fork channel
     * required to handle a request does not exist on the recipient node.
     * @param response a message response
     * @return true, if the response indicates a missing fork stack or channel.
     */
    boolean isUnknownForkResponse(Message response);
}
