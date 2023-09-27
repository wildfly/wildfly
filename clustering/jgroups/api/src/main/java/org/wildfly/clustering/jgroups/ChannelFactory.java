/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups;

import org.jgroups.JChannel;

/**
 * Factory for creating JGroups channels.
 * @author Paul Ferraro
 */
public interface ChannelFactory {
    /**
     * Creates a JGroups channel
     * @param id the unique identifier of this channel
     * @return a JGroups channel
     * @throws Exception if there was a failure setting up the protocol stack
     */
    JChannel createChannel(String id) throws Exception;
}
