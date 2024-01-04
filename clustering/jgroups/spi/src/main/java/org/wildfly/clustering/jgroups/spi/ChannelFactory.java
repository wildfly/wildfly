/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jgroups.Message;

/**
 * Factory for creating JGroups channels.
 * @author Paul Ferraro
 */
public interface ChannelFactory extends org.wildfly.clustering.jgroups.ChannelFactory {

    /**
     * Returns the protocol stack configuration of this channel factory.
     * @return the protocol stack configuration of this channel factory
     */
    ProtocolStackConfiguration getProtocolStackConfiguration();

    /**
     * Determines whether or not the specified message response indicates the fork stack or fork channel
     * required to handle a request does not exist on the recipient node.
     * @param response a message response
     * @return true, if the response indicates a missing fork stack or channel.
     */
    boolean isUnknownForkResponse(Message response);
}
